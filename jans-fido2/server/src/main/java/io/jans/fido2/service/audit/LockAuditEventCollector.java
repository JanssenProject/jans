/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.google.common.collect.Lists;

import io.jans.fido2.model.audit.LockAuditEvent;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Buffers passkey registration/authentication audit events and flushes them to the Lock Server in
 * batches on a schedule, rather than one HTTP call per ceremony. Callers ({@code AttestationService}
 * / {@code AssertionService}, added in the sub-issues that build on this one) only ever call
 * {@link #collect}, which is non-blocking and never throws — a Lock Server outage or a
 * misconfiguration must never be visible on the registration/authentication request path.
 * <p>
 * Mirrors {@code AbandonedCeremonyTimer}'s timer/event/reentrancy-guard shape rather than
 * {@code ScheduledExecutorService}, matching how the rest of jans-fido2 schedules recurring work.
 */
@ApplicationScoped
@Named
public class LockAuditEventCollector {

	private static final int DEFAULT_FLUSH_INTERVAL = 20;

	/**
	 * ponytail: a hardcoded ceiling, not a config property — raise it (or make it configurable) if a
	 * real deployment's sustained failure rate needs more headroom than this. Bounds how long a Lock
	 * Server outage can keep growing the buffer: delivery (discovery + token grant + POST, each with
	 * a 5s/10s timeout) can take up to ~45s, during which the scheduler skips later flushes while
	 * collect() keeps accepting — including a DENY event per rejected/unauthenticated request, so an
	 * unbounded queue is a real memory-growth path under load, not just a theoretical one.
	 */
	static final int MAX_BUFFER_SIZE = 1000;

	/**
	 * ponytail: hardcoded, not configurable — raise it if the Lock Server's actual payload-size limit
	 * turns out to tolerate more. Caps each delivery POST so a large drained batch can't produce one
	 * oversized payload that gets the whole batch rejected; multiple smaller POSTs fail independently.
	 */
	static final int MAX_BATCH_SIZE = 50;

	/**
	 * Every full-buffer drop is counted, but only every Nth one is logged — under sustained overload
	 * every collect() call would otherwise log at WARN on the request thread, turning a Lock Server
	 * outage into a logging-volume problem of its own.
	 */
	static final int DROP_LOG_EVERY_N = 100;

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private LockAuditClient lockAuditClient;

	@Inject
	private LockAuditTokenService lockAuditTokenService;

	@Inject
	private Event<TimerEvent> timerEvent;

	private final BlockingQueue<LockAuditEvent> buffer = new LinkedBlockingQueue<>(MAX_BUFFER_SIZE);
	private final AtomicLong droppedEventCount = new AtomicLong();

	private AtomicBoolean isActive;

	/**
	 * Buffers {@code event} for the next flush. A no-op, not an error, when delivery is disabled —
	 * otherwise a deployment that never enabled this feature would grow the buffer unboundedly with
	 * events nothing will ever drain. Also a no-op, logged rather than thrown, once the buffer is at
	 * {@link #MAX_BUFFER_SIZE} — dropping the newest event is preferable to unbounded growth, and this
	 * must never make the caller's registration/authentication request fail or block.
	 */
	public void collect(LockAuditEvent event) {
		if (event == null || !isEnabled()) {
			return;
		}
		if (!buffer.offer(event)) {
			long dropped = droppedEventCount.incrementAndGet();
			if (dropped == 1 || dropped % DROP_LOG_EVERY_N == 0) {
				log.warn("Lock audit buffer full ({} events), dropping events (total dropped: {})", MAX_BUFFER_SIZE, dropped);
			}
		}
	}

	/** Package-visible for tests; not exposed further since nothing else reads it yet. */
	long droppedEventCount() {
		return droppedEventCount.get();
	}

	public void initTimer() {
		log.info("Initializing Lock audit event flush timer");
		this.isActive = new AtomicBoolean(false);

		// Scheduled regardless of whether delivery is currently enabled, same reasoning as
		// AbandonedCeremonyTimer: the configuration is reloadable, and a timer that was never
		// scheduled could not observe it being switched on later without a restart.
		int interval = effectiveFlushInterval();
		timerEvent.fire(new TimerEvent(new TimerSchedule(interval, interval), new LockAuditFlushEvent() {
		}, Scheduled.Literal.INSTANCE));

		log.info("Initialized Lock audit event flush timer with interval {}s", interval);
	}

	@Asynchronous
	public void process(@Observes @Scheduled LockAuditFlushEvent lockAuditFlushEvent) {
		if (this.isActive == null || this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			processImpl();
		} finally {
			this.isActive.set(false);
		}
	}

	/**
	 * Drains the buffer and delivers it in {@link #MAX_BATCH_SIZE}-sized chunks, all under a single
	 * access token obtained once for the whole drain. Package-visible so a test can drive one pass
	 * without standing up the timer.
	 * <p>
	 * Always drains, even when disabled: a config toggle raced against an in-flight collect() must
	 * not leave events sitting in the buffer forever. Each chunk's delivery failure is caught
	 * independently, not left to the caller (a scheduled event with nothing sensible to do with a
	 * thrown exception) — and independently of the other chunks, so one oversized or rejected chunk
	 * does not stop the rest of the drained batch from being attempted.
	 */
	void processImpl() {
		List<LockAuditEvent> batch = drain();
		if (batch.isEmpty()) {
			return;
		}

		if (!isEnabled()) {
			log.debug("Lock audit delivery disabled, discarding {} buffered event(s)", batch.size());
			return;
		}

		// Obtained once per drain, not once per chunk: a full 1000-event drain split into 20 chunks of
		// 50 previously meant 20 client-credentials grants against the auth server for one flush.
		String accessToken = lockAuditTokenService.getAccessToken();
		if (accessToken == null) {
			log.warn("Failed to obtain a Lock audit access token, dropping {} buffered event(s)", batch.size());
			return;
		}

		for (List<LockAuditEvent> chunk : Lists.partition(batch, MAX_BATCH_SIZE)) {
			try {
				lockAuditClient.postBatch(chunk, accessToken);
			} catch (Exception e) {
				log.warn("Failed to deliver {} Lock audit event(s), dropping chunk", chunk.size(), e);
			}
		}
	}

	private List<LockAuditEvent> drain() {
		List<LockAuditEvent> batch = new ArrayList<>();
		buffer.drainTo(batch);
		return batch;
	}

	private boolean isEnabled() {
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		return fido2Configuration != null && fido2Configuration.isLockAuditEnabled();
	}

	private int effectiveFlushInterval() {
		Fido2Configuration fido2Configuration = appConfiguration.getFido2Configuration();
		int interval = fido2Configuration == null ? 0 : fido2Configuration.getLockAuditFlushInterval();
		return interval > 0 ? interval : DEFAULT_FLUSH_INTERVAL;
	}
}

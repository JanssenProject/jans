/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

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

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private LockAuditClient lockAuditClient;

	@Inject
	private Event<TimerEvent> timerEvent;

	private final Queue<LockAuditEvent> buffer = new ConcurrentLinkedQueue<>();

	private AtomicBoolean isActive;

	/**
	 * Buffers {@code event} for the next flush. A no-op, not an error, when delivery is disabled —
	 * otherwise a deployment that never enabled this feature would grow the buffer unboundedly with
	 * events nothing will ever drain.
	 */
	public void collect(LockAuditEvent event) {
		if (event == null || !isEnabled()) {
			return;
		}
		buffer.add(event);
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
	 * Drains the buffer and delivers it as one batch. Package-visible so a test can drive one pass
	 * without standing up the timer.
	 * <p>
	 * Always drains, even when disabled: a config toggle raced against an in-flight collect() must
	 * not leave events sitting in the buffer forever. Delivery failure is caught here, not left to
	 * the caller, since the caller is a scheduled event with nothing sensible to do with a thrown
	 * exception.
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

		try {
			lockAuditClient.postBatch(batch);
		} catch (Exception e) {
			log.warn("Failed to deliver {} Lock audit event(s), dropping batch", batch.size(), e);
		}
	}

	private List<LockAuditEvent> drain() {
		List<LockAuditEvent> batch = new ArrayList<>();
		LockAuditEvent event;
		while ((event = buffer.poll()) != null) {
			batch.add(event);
		}
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

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.service.event.TraceReceiptRepairEvent;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Periodically settles stale {@code PENDING} receipt-chain allocation claims (design §9, TRACE
 * MVP design decision D-8): a claim with no matching record after
 * {@code pendingReceiptTimeoutSeconds} almost certainly lost its node before step 5/6 of the
 * protocol completed, so it is resolved here instead of staying {@code PENDING} forever.
 *
 * <p>Structured like {@link io.jans.lock.service.stat.StatTimer}: {@link #initTimer()} fires the
 * recurring {@link TraceReceiptRepairEvent}, {@link #process} is the CDI-scheduled observer, and
 * {@link #repairPendingReceipts()} is the plain processing method a test calls directly, without
 * going through the timer/event machinery.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceReceiptRepairTimer {

	private static final int TIMER_TICK_INTERVAL_IN_SECONDS = 60;

	private static final int BATCH_LIMIT = 500;

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private TraceStore store;

	@Inject
	private TraceReceiptChain receiptChain;

	private AtomicBoolean isActive;

	private long lastFinishedTime;

	@Asynchronous
	public void initTimer() {
		log.info("Initializing TRACE Receipt Repair Timer");

		this.isActive = new AtomicBoolean(false);

		timerEvent.fire(new TimerEvent(new TimerSchedule(TIMER_TICK_INTERVAL_IN_SECONDS, TIMER_TICK_INTERVAL_IN_SECONDS),
				new TraceReceiptRepairEvent(), Scheduled.Literal.INSTANCE));

		this.lastFinishedTime = System.currentTimeMillis();
		log.info("Initialized TRACE Receipt Repair Timer");
	}

	@Asynchronous
	public void process(@Observes @Scheduled TraceReceiptRepairEvent event) {
		if (this.isActive.get()) {
			return;
		}
		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			if (!allowToRun()) {
				return;
			}
			repairPendingReceipts();
			this.lastFinishedTime = System.currentTimeMillis();
		} catch (Exception ex) {
			log.error("Exception happened while repairing TRACE receipts", ex);
		} finally {
			this.isActive.set(false);
		}
	}

	private boolean allowToRun() {
		if (!appConfiguration.getTraceConfiguration().isEnabled()) {
			return false;
		}
		long intervalMs = appConfiguration.getTraceConfiguration().getReceiptRepairIntervalSeconds() * 1000L;
		long timeDiff = System.currentTimeMillis() - this.lastFinishedTime;
		return timeDiff >= intervalMs;
	}

	/**
	 * Loads every {@code PENDING} receipt older than {@code pendingReceiptTimeoutSeconds} and
	 * settles it: {@code COMMITTED} if its record row now exists, {@code VOID} otherwise. Called
	 * directly by tests, bypassing the CDI timer event.
	 */
	public void repairPendingReceipts() {
		long cutoffMs = System.currentTimeMillis()
				- appConfiguration.getTraceConfiguration().getPendingReceiptTimeoutSeconds() * 1000L;
		List<TraceReceiptEntry> pending = store.findPendingReceiptsOlderThan(cutoffMs, BATCH_LIMIT);

		int committed = 0;
		int voided = 0;
		for (TraceReceiptEntry receipt : pending) {
			RecordIdentity identity = new RecordIdentity(receipt.getDomainId(), receipt.getProducerId(),
					receipt.getRecordId());
			if (store.findRecord(identity).isPresent()) {
				receiptChain.commit(receipt.getDomainId(), receipt.getReceiptSeq());
				committed++;
			} else {
				receiptChain.voidClaim(receipt.getDomainId(), receipt.getReceiptSeq());
				voided++;
			}
		}

		if (!pending.isEmpty()) {
			log.info("TRACE receipt repair: examined={}, committed={}, voided={}", pending.size(), committed, voided);
		}
	}

}

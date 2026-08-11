/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.shared.MetricService;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2AuthenticationStatus;
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
 * Relabels assertion ceremonies that lapsed without ever being completed.
 * <p>
 * A ceremony is written as {@code pending} when the options are issued and flipped to
 * {@code authenticated} when the assertion is verified. If the user cancels, closes the sheet, or
 * simply gives up, nothing is ever posted back — so without this sweep the row stays {@code pending}
 * until the cleaner deletes it, and an abandoned ceremony is indistinguishable first from one still
 * in flight and then from one that never happened at all.
 * <p>
 * The sweep is deliberately <em>not</em> coordinated across nodes, so abandonment is counted at least
 * once rather than exactly once. Two locking approaches were examined and rejected: the metrics
 * aggregation scheduler's cluster lock is only initialised behind that scheduler's own enabled check,
 * so with aggregation disabled it silently reports success on every node; and a dedicated cluster
 * node type would allocate an index into a base DN that other components already populate. What
 * bounds the error is the transition itself — it only fires from {@code pending}, so a row another
 * node has already claimed is not matched again and the stored value converges regardless of who
 * writes it. Only the emitted metric can duplicate, and only for a batch two nodes read before either
 * writes it back; single-node deployments are exact.
 *
 * @author Janssen Project
 */
@ApplicationScoped
@Named
public class AbandonedCeremonyTimer {

	/**
	 * Ceilings one sweep. Abandonment is the highest-volume outcome — conditional-UI ceremonies start
	 * on virtually every login page load — so a backlog is bounded per pass rather than read at once.
	 */
	public static final int BATCH_SIZE = 1000;

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private AuthenticationPersistenceService authenticationPersistenceService;

	@Inject
	private MetricService metricService;

	@Inject
	private Event<TimerEvent> timerEvent;

	private AtomicBoolean isActive;

	public void initTimer() {
		log.info("Initializing Abandoned Ceremony Timer");
		this.isActive = new AtomicBoolean(false);

		// Scheduled regardless of whether recording is currently enabled. The configuration is
		// reloadable, and a timer that was never scheduled could not observe it being switched on —
		// which would make the property restart-only. Each pass re-checks and does nothing while off.
		int interval = AbandonedCeremonyPolicy.effectiveSweepInterval(appConfiguration.getFido2Configuration());
		if (AbandonedCeremonyPolicy.isSweepIntervalOverridden(appConfiguration.getFido2Configuration())) {
			log.warn(
					"abandonedRequestSweepInterval {} is not usable with unfinishedRequestExpiration {}; sweeping every {}s instead, otherwise a ceremony could lapse and be deleted between two passes",
					appConfiguration.getFido2Configuration().getAbandonedRequestSweepInterval(),
					appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration(), interval);
		}

		timerEvent.fire(new TimerEvent(new TimerSchedule(interval, interval), new AbandonedCeremonyEvent() {
		}, Scheduled.Literal.INSTANCE));

		log.info("Initialized Abandoned Ceremony Timer with interval {}s (recording enabled: {})", interval,
				isSweepEnabled());
	}

	@Asynchronous
	public void process(@Observes @Scheduled AbandonedCeremonyEvent abandonedCeremonyEvent) {
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
	 * Claims every ceremony whose window has elapsed. Package-visible so a test can drive one pass
	 * without standing up the timer.
	 */
	void processImpl() {
		try {
			// Re-read on every pass: the configuration is reloadable, and a sweep that kept a stale
			// window would relabel ceremonies that are still legitimately open.
			if (!isSweepEnabled()) {
				return;
			}

			Date lapsedBefore = lapsedBefore();
			int abandoned = 0;
			for (String baseDn : authenticationPersistenceService.getCeremonyBaseDns()) {
				abandoned += sweep(baseDn, lapsedBefore);
			}

			if (abandoned > 0) {
				log.debug("Marked {} lapsed assertion ceremonies as abandoned", abandoned);
			}
		} catch (Exception e) {
			log.error("Failed to sweep abandoned assertion ceremonies.", e);
		}
	}

	private int sweep(String baseDn, Date lapsedBefore) {
		List<Fido2AuthenticationEntry> lapsed;
		try {
			lapsed = authenticationPersistenceService.findLapsedPendingCeremonies(baseDn, lapsedBefore, BATCH_SIZE);
		} catch (Exception e) {
			// One unreadable subtree must not stop the other from being swept.
			log.warn("Failed to read lapsed assertion ceremonies from {}: {}", baseDn, e.getMessage());
			return 0;
		}

		int abandoned = 0;
		for (Fido2AuthenticationEntry entry : lapsed) {
			if (markAbandoned(entry)) {
				abandoned++;
			}
		}
		return abandoned;
	}

	/**
	 * Claims one ceremony.
	 * <p>
	 * The status is re-checked on the loaded entry rather than transitioned conditionally in the
	 * store, because {@code PersistenceEntryManager} exposes only an unconditional merge. What keeps
	 * that from mattering is that the sweep and a live verification operate on disjoint ceremonies:
	 * this only selects ceremonies already past {@code unfinishedRequestExpiration}, and a ceremony
	 * past that point is rejected by {@code AssertionService.verify} rather than completed. So a sweep
	 * cannot overwrite an outcome a concurrent verification is about to write — the verification would
	 * have to have resolved before the ceremony lapsed, in which case the status check below sees it.
	 *
	 * @return true when this pass is the one that claimed the ceremony
	 */
	private boolean markAbandoned(Fido2AuthenticationEntry entry) {
		try {
			Fido2AuthenticationData authenticationData = entry.getAuthenticationData();
			if (authenticationData == null
					|| authenticationData.getStatus() != Fido2AuthenticationStatus.pending) {
				// Already resolved — by a verification that landed before the ceremony lapsed, or by
				// another node's sweep between the read above and now.
				return false;
			}

			authenticationData.setStatus(Fido2AuthenticationStatus.abandoned);
			entry.setExpiration(appConfiguration.getFido2Configuration().getAbandonedRequestExpiration());
			authenticationPersistenceService.update(entry);

			recordAbandonment(entry, authenticationData);
			return true;
		} catch (Exception e) {
			// A single unwritable row must not abort the batch.
			log.warn("Failed to mark assertion ceremony {} as abandoned: {}", entry.getId(), e.getMessage());
			return false;
		}
	}

	private void recordAbandonment(Fido2AuthenticationEntry entry, Fido2AuthenticationData authenticationData) {
		try {
			Date creationDate = entry.getCreationDate();
			long ceremonyStartTime = creationDate != null ? creationDate.getTime() : System.currentTimeMillis();
			metricService.recordPasskeyAuthenticationAbandoned(authenticationData.getUsername(), ceremonyStartTime);
		} catch (Exception e) {
			// The row is already labelled; losing the metric must not undo or retry that.
			log.debug("Failed to record abandonment metric: {}", e.getMessage());
		}
	}

	/**
	 * Ceremonies issued at or before this instant have outlived their window.
	 */
	private Date lapsedBefore() {
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.SECOND, -appConfiguration.getFido2Configuration().getUnfinishedRequestExpiration());
		return calendar.getTime();
	}

	private boolean isSweepEnabled() {
		return appConfiguration.getFido2Configuration() != null
				&& appConfiguration.getFido2Configuration().isRecordAbandonedAssertions();
	}
}

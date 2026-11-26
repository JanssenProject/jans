/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.attestation;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cdi.event.AttestationChallengeRepairEvent;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Returns attestation challenges whose processing lease expired (the appraising node crashed) to {@code pending}
 * (design §9.3c repair timer). Safe on every cluster node: each transition is a compare-and-set.
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class AttestationChallengeRepairTimer {

    private static final int TIMER_TICK_SECONDS = 10;

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AttestationChallengeService challengeService;

    @Inject
    private AttestationStore store;

    private final AtomicBoolean active = new AtomicBoolean(false);
    private volatile long lastFinishedTime;

    public void initTimer() {
        log.debug("Initializing attestation challenge repair timer");
        if (appConfiguration.isFeatureEnabled(FeatureFlagType.ATTESTATION) && !store.isTransactional()) {
            log.error("The attestation endpoint requires SQL persistence (transactions and row locks). "
                    + "It stays unavailable on this persistence backend.");
        }
        timerEvent.fire(new TimerEvent(new TimerSchedule(TIMER_TICK_SECONDS, TIMER_TICK_SECONDS),
                new AttestationChallengeRepairEvent(), Scheduled.Literal.INSTANCE));
        lastFinishedTime = System.currentTimeMillis();
    }

    @Asynchronous
    public void process(@Observes @Scheduled AttestationChallengeRepairEvent event) {
        if (!active.compareAndSet(false, true)) {
            return;
        }
        try {
            if (isDue()) {
                int repaired = challengeService.repairExpiredLeases();
                if (repaired > 0) {
                    log.debug("Repaired {} attestation challenges with expired processing leases", repaired);
                }
            }
        } catch (Exception e) {
            log.error("Attestation challenge repair failed", e);
        } finally {
            active.set(false);
            lastFinishedTime = System.currentTimeMillis();
        }
    }

    boolean isDue() {
        if (!appConfiguration.isFeatureEnabled(FeatureFlagType.ATTESTATION) || !store.isTransactional()) {
            return false;
        }
        long intervalMillis = Math.max(1, appConfiguration.getAttestationChallengeRepairIntervalSeconds()) * 1000L;
        return System.currentTimeMillis() - lastFinishedTime >= intervalMillis;
    }
}

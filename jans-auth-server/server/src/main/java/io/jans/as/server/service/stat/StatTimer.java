package io.jans.as.server.service.stat;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cdi.event.StatEvent;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class StatTimer {

    private static final int TIMER_TICK_INTERVAL_IN_SECONDS = 60; // 1 min
    private static final int TIMER_INTERVAL_IN_SECONDS = 15 * 60; // 15 min

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StatService statService;

    private AtomicBoolean isActive;
    private long lastFinishedTime;

    @Asynchronous
    public void initTimer() {
        log.info("Initializing Stat Service Timer");

        this.isActive = new AtomicBoolean(false);

        timerEvent.fire(new TimerEvent(new TimerSchedule(TIMER_TICK_INTERVAL_IN_SECONDS, TIMER_TICK_INTERVAL_IN_SECONDS), new StatEvent(), Scheduled.Literal.INSTANCE));

        this.lastFinishedTime = System.currentTimeMillis();
        log.info("Initialized Stat Service Timer");
    }

    @Asynchronous
    public void process(@Observes @Scheduled StatEvent event) {
        if (!appConfiguration.isFeatureEnabled(FeatureFlagType.STAT)) {
            return;
        }

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
            statService.updateStat();
            this.lastFinishedTime = System.currentTimeMillis();
        } catch (Exception ex) {
            log.error("Exception happened while updating stat", ex);
        } finally {
            this.isActive.set(false);
        }
    }

    private boolean allowToRun() {
        int interval = appConfiguration.getStatTimerIntervalInSeconds();
        if (interval < 0) {
            log.info("Stat Timer is disabled.");
            log.warn("Stat Timer Interval (statTimerIntervalInSeconds in server configuration) is negative which turns OFF statistic on the server. Please set it to positive value if you wish it to run.");
            return false;
        }
        if (interval == 0)
            interval = TIMER_INTERVAL_IN_SECONDS;

        long timerInterval = interval * 1000L;

        long timeDiff = System.currentTimeMillis() - this.lastFinishedTime;

        return timeDiff >= timerInterval;
    }
}

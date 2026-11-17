/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.service;

import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.QuartzSchedulerManager;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import org.slf4j.Logger;

/**
 * Schedules the three jobs of {@link FacadeJobService}, each on its own timer with its own interval (one hour by
 * default): the first sends the TRACE records to Lock, the second fetches them back, the third trains FACADE.
 */
@ApplicationScoped
public class FacadeTimer {

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private QuartzSchedulerManager quartzSchedulerManager;

    @Inject
    private FacadeJobService jobService;

    /**
     * Runs after Config API's own startup observer, which starts the Quartz scheduler. Observers without a priority
     * run at {@code APPLICATION + 500}, so this one runs later.
     */
    public void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION + 1000)
            @Initialized(ApplicationScoped.class) Object init) {
        if (!quartzSchedulerManager.isInitialized()) {
            log.error("Quartz scheduler is not initialized; FACADE jobs are not scheduled");
            return;
        }
        FacadeSettings settings = FacadeSettings.load();
        if (!settings.jobEnabled()) {
            log.info("FACADE jobs are disabled (facade.job.enabled=false)");
            return;
        }
        log.info("Scheduling FACADE jobs. input {}, output {}", settings.inputDir(), settings.outputDir());
        schedule("send", settings.sendInitialDelaySeconds(), settings.sendIntervalSeconds(),
                new FacadeSendTimerEvent());
        schedule("collect", settings.collectInitialDelaySeconds(), settings.collectIntervalSeconds(),
                new FacadeCollectTimerEvent());
        schedule("train", settings.trainInitialDelaySeconds(), settings.trainIntervalSeconds(),
                new FacadeTrainTimerEvent());
    }

    private void schedule(String name, int delay, int interval, Object event) {
        log.info("FACADE {} job: first run in {}s, then every {}s", name, delay, interval);
        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), event, Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void processSend(@Observes @Scheduled FacadeSendTimerEvent event) {
        jobService.send();
    }

    @Asynchronous
    public void processCollect(@Observes @Scheduled FacadeCollectTimerEvent event) {
        jobService.collect();
    }

    @Asynchronous
    public void processTrain(@Observes @Scheduled FacadeTrainTimerEvent event) {
        jobService.train();
    }
}

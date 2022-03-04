/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.timer;

import java.util.Date;
import java.util.UUID;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.JobShedule;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.init.Initializable;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.slf4j.Logger;

/**
 * @author Yuriy MovchanDate: 04/04/2017
 */
@ApplicationScoped
public class QuartzSchedulerManager extends Initializable {

    public static final String QUARTZ_PROPERTY_FILE_NAME = "quartz.properties";

    @Inject
    private Logger log;

    @Inject
    private RequestJobListener jobListener;

    @Inject
    private JobExecutionFactory jobFactory;

    private Scheduler scheduler;

    protected void initInternal() {
        try {
            StdSchedulerFactory factory = new StdSchedulerFactory();
            factory.initialize(QUARTZ_PROPERTY_FILE_NAME);

            scheduler = factory.getScheduler();

            // Register job listener to bound request context to every job
            // execution
            scheduler.getListenerManager().addJobListener(jobListener, EverythingMatcher.allJobs());
            // Replace default job factory
            scheduler.setJobFactory(jobFactory);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to initialize Quartz scheduler manager", ex);
        }

        log.info("Quartz scheduler manager initialized");
    }

    public void schedule(JobShedule jobShedule) {
        schedule(jobShedule.getJobDetail(), jobShedule.getTrigger());
    }

    public void schedule(JobDetail jobDetail, Trigger trigger) {
        checkInitialized();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to shedule Quartz job", ex);
        }
    }

    public void schedule(@Observes TimerEvent timerEvent) {
        checkInitialized();

        JobDataMap dataMap = new JobDataMap();
        dataMap.put(TimerJob.KEY_TIMER_EVENT, timerEvent);

        String uuid = UUID.randomUUID().toString();

        JobDetail timerJob = JobBuilder.newJob(TimerJob.class).withIdentity(TimerJob.class.getSimpleName() + "_" + uuid, TimerJob.TIMER_JOB_GROUP)
                .usingJobData(dataMap).build();

        TimerSchedule timerSchedule = timerEvent.getSchedule();
        Date triggerStartTime = new Date(System.currentTimeMillis() + timerSchedule.getDelay() * 1000L);
        Trigger timerTrigger = TriggerBuilder.newTrigger().withIdentity(uuid, TimerJob.TIMER_JOB_GROUP).startAt(triggerStartTime)
                .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(timerSchedule.getInterval())).build();

        try {
            scheduler.scheduleJob(timerJob, timerTrigger);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to schedule Timer Event", ex);
        }
    }

    public void start() {
        if (!isInitialized()) {
            super.init();
        }

        checkInitialized();

        try {
            scheduler.start();
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to start Quartz sheduler", ex);
        }

        log.info("Quartz scheduler started");
    }

    public void standby() {
        checkInitialized();

        try {
            scheduler.standby();
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to set Quartz sheduler stand-by mode", ex);
        }

        log.info("Quartz scheduler in stand-by mode");
    }

    public void clear() {
        checkInitialized();

        try {
            scheduler.clear();
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to clear Quartz sheduler", ex);
        }

        log.info("Quartz scheduler is clear now");
    }

    @PreDestroy
    public void destroy() {
        if (!isInitialized()) {
            return;
        }

        try {
            scheduler.shutdown();
            log.info("Quartz scheduler manager destroyed");
        } catch (SchedulerException ex) {
            log.warn("Cannot shutdown quartz scheduler!", ex);
        }
    }

    public boolean isInitialized() {
        return scheduler != null;
    }

    private void checkInitialized() {
        if (!isInitialized()) {
            throw new IllegalStateException("Quartz scheduler manager not initialized!");
        }
    }

}

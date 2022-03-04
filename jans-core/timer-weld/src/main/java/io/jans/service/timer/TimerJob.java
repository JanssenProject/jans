/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.timer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import io.jans.service.timer.event.TimerEvent;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan Date: 04/13/2017
 */
@Dependent
public class TimerJob implements Job {

    public static final String KEY_TIMER_EVENT = TimerEvent.class.getName();
    public static final String TIMER_JOB_GROUP = "TimerJobGroup";

    @Inject
    private Logger log;

    @Inject
    private BeanManager beanManager;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            TimerEvent timerEvent = (TimerEvent) context.getJobDetail().getJobDataMap().get(KEY_TIMER_EVENT);
            if (timerEvent == null) {
                return;
            }

            log.debug("Fire timer event [{}] with qualifiers {} from instance {}", timerEvent.getTargetEvent().getClass().getName(),
                    timerEvent.getQualifiers(), System.identityHashCode(this));

            beanManager.fireEvent(timerEvent.getTargetEvent(), timerEvent.getQualifiers());
        } catch (Exception ex) {
            throw new JobExecutionException(ex);
        }
    }

}

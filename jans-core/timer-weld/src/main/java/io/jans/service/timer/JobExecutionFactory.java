/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.timer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * @author Yuriy Movchan Date: 04/04/2017
 */
@ApplicationScoped
public class JobExecutionFactory implements JobFactory {

    @Inject
    private JobExecutionDelegate jobExecutionDelegate;

    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        return jobExecutionDelegate;
    }

}

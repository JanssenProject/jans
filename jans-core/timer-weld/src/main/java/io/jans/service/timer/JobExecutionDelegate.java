/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.timer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author Yuriy Movchan Date: 04/04/2017
 */
@Dependent
public class JobExecutionDelegate implements Job {

    @Inject
    private Instance<Job> jobInstance;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        Instance<? extends Job> job = jobInstance.select(context.getJobDetail().getJobClass());

        if (job.isAmbiguous()) {
            throw new IllegalStateException("Cannot produce job: ambiguous instance");
        }

        if (job.isUnsatisfied()) {
            throw new IllegalStateException("Cannot produce job: unsatisfied instance");
        }

        job.get().execute(context);
    }

}

package io.jans.kc.scheduler.job;

import io.jans.kc.scheduler.job.RecurringJobSpec;

public interface JobScheduler {
    public JobScheduler start();
    public JobScheduler stop();
    public JobScheduler scheduleRecurringJob(RecurringJobSpec spec);
}
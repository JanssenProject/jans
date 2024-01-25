package io.jans.kc.scheduler.job;

import io.jans.kc.scheduler.job.JobSchedulerException;

public class JobSpec {

    protected String name;
    protected Class<? extends Job> jobclazz;

    public JobSpec(String name, Class<? extends Job> jobclazz) {
        this.name = name;
        this.jobclazz = jobclazz;
    }

    public String getName() {

        return this.name;
    }

    public Class<? extends Job> getJobClass() {

        return this.jobclazz;
    }

    protected void validate() {

        if(this.name == null || this.name.isEmpty() ) {
            throw new JobSchedulerException("Job name was not specified");
        }

        if(this.jobclazz == null) {
            throw new JobSchedulerException("Job class not specified");
        }
    }
}

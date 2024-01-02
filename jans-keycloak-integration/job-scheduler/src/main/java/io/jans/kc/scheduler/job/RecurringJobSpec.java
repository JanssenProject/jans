package io.jans.kc.scheduler.job;

import io.jans.kc.scheduler.job.service.JobSchedulerException;

public class RecurringJobSpec extends JobSpec {

    private static final Integer DEFAULT_SCHEDULING_INTERVAL = 5; // in seconds 
    private Integer schedulingInterval;
    private boolean repeatForever;

    private RecurringJobSpec(String name, Class<? extends RecurringJob> jobclazz) {

        super(name,jobclazz);
        this.schedulingInterval = DEFAULT_SCHEDULING_INTERVAL;
        this.repeatForever = true;
    }

    private RecurringJobSpec(String name, Class<? extends RecurringJob> jobclazz,Integer schedulingInterval) {
        super(name,jobclazz);
        this.schedulingInterval = schedulingInterval;
        this.repeatForever = true;
    }

    private RecurringJobSpec(String name,Class<? extends RecurringJob> jobclazz,Integer schedulingInterval, boolean repeatForever) {
        super(name,jobclazz);
        this.schedulingInterval = schedulingInterval;
        this.repeatForever = repeatForever;
    }

    public Integer schedulingInterval() {

        return schedulingInterval;
    }

    public boolean repeatForever() {

        return repeatForever;
    }

    public static Builder builder() {

        return new Builder();
    }

    public static class Builder {

        private RecurringJobSpec spec;

        private Builder() {
            this.spec = new RecurringJobSpec(null, null);
        }

        public Builder name(String name) {

            this.spec.name = name;
            return this;
        }


        public Builder jobClass(Class<? extends Job> jobclazz) {

            this.spec.jobclazz = jobclazz;
            return this;
        }

        public Builder repeatForever(boolean forever) {

            this.spec.repeatForever = forever;
            return this;
        }

        public Builder scheduleIntervalInSeconds(Integer seconds) {

            this.spec.schedulingInterval = seconds;
            return this;
        }

        public RecurringJobSpec build() {

            if(this.spec.name == null) {
                throw new JobSchedulerException("Job name not specified ");
            }

            if(this.spec.jobclazz == null) {
                throw new JobSchedulerException("Job class not specified");
            }
            return spec;
        }
    }
}

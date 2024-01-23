package io.jans.kc.scheduler.job;

import io.jans.kc.scheduler.job.JobSchedulerException;
import java.time.Duration;

public class RecurringJobSpec extends JobSpec {

    private Duration schedulingInterval;
    private boolean repeatForever;
    private Integer repeatCount;

    private RecurringJobSpec(String name, Class<? extends RecurringJob> jobclazz) {
        super(name,jobclazz);
        this.schedulingInterval = null;
        this.repeatForever = true;
        this.repeatCount = 0;
    }
    private RecurringJobSpec(String name, Class<? extends RecurringJob> jobclazz,
        Duration schedulingInterval) {
        super(name,jobclazz);
        this.schedulingInterval = schedulingInterval;
        this.repeatForever = true;
        this.repeatCount = 0;
    }

    private RecurringJobSpec(String name, Class<? extends RecurringJob> jobclazz,
        Duration schedulingInterval, boolean repeatForever) {
        super(name,jobclazz);
        this.schedulingInterval = schedulingInterval;
        this.repeatForever = repeatForever;
        this.repeatCount = 0;
    }

    public Duration schedulingInterval() {

        return schedulingInterval;
    }

    public boolean repeatForever() {

        return repeatForever;
    }

    public Integer repeatCount() {

        return this.repeatCount;
    }

    public static Builder builder() {

        return new Builder();
    }

    public static class Builder {

        private RecurringJobSpec spec;

        private Builder() {
            this.spec = new RecurringJobSpec(null, null,null);
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

        public Builder schedulingInterval(Duration interval) {

            this.spec.schedulingInterval = interval;
            return this;
        }

        public Builder repeatCount(Integer count) {

            this.spec.repeatCount = count;
            return this;
        }

        public RecurringJobSpec build() {

            if(this.spec.name == null) {
                throw new JobSchedulerException("Job name not specified ");
            }

            if(this.spec.jobclazz == null) {
                throw new JobSchedulerException("Job class not specified");
            }

            if(this.spec.schedulingInterval == null || this.spec.schedulingInterval.isZero() || this.spec.schedulingInterval.isNegative()) {
                throw new JobSchedulerException("Invalid job scheduling interval value specified");
            }
            
            if(this.spec.repeatCount < 0) {
                throw new JobSchedulerException("Job repeat count must be greater than zero");
            }
            return spec;
        }
    }
}

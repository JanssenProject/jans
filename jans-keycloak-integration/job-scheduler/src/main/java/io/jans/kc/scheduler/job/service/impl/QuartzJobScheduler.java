package io.jans.kc.scheduler.job.service.impl;


import io.jans.kc.scheduler.job.RecurringJobSpec;
import io.jans.kc.scheduler.job.service.JobScheduler;
import io.jans.kc.scheduler.job.service.JobSchedulerException;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;


import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.quartz.spi.ThreadPool;
import org.quartz.impl.DirectSchedulerFactory;

public class QuartzJobScheduler implements JobScheduler {
    
    private static final Integer DEFAULT_THREAD_POOL_SIZE = 3;

    private Scheduler quartzScheduler;

    private QuartzJobScheduler(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    @Override
    public JobScheduler start() {

        try {
            quartzScheduler.start();
            return this;
        }catch(SchedulerException e) {
            throw new JobSchedulerException("Unable to start job scheduler",e);
        }
    }

    @Override
    public JobScheduler stop() {

        try {
            quartzScheduler.shutdown(true);
        }catch(SchedulerException e) {
            throw new JobSchedulerException("Unable to stop job scheduler",e);
        }
        return this;
    }

    @Override
    public JobScheduler scheduleRecurringJob(RecurringJobSpec spec) {
        
        try {
            
            Trigger trigger = newTrigger()
                .withIdentity(recurringJobTriggerKey(spec))
                .withSchedule(simpleSchedule().repeatSecondlyForever(spec.schedulingInterval()))
                .startNow()
                .build();
            
            JobDetail jobdetail = newJob(QuartzJobWrapper.class)
                    .withIdentity(spec.getName())
                    .build();
            
            quartzScheduler.scheduleJob(jobdetail,trigger);
            return this;
        }catch(SchedulerException e) {
            throw new JobSchedulerException("Unable to schedule recurring job",e);
        }
    }

    private TriggerKey recurringJobTriggerKey(RecurringJobSpec spec) {

        String name = String.format("r_job_%s",spec.getName());
        String group = String.format("r_job_grp_%s",spec.getName());
        return triggerKey(name,group);
    }


    public static Builder builder() {

        return new Builder();
    }

    public static final class Builder {

        private String  name;
        private String  instanceId;
        private Integer threadPoolSize;

        private Builder() {

            this.name = null;
            this.instanceId = null;
            this.threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        }

        public Builder name(String name) {

            this.name = name;
            return this;
        }

        public Builder instanceId(String instanceId) {

            this.instanceId = instanceId;
            return this;
        }

        public Builder threadPoolSize(Integer threadPoolSize) {

            this.threadPoolSize = threadPoolSize;
            return this;
        }


        public final QuartzJobScheduler build() {

            if (this.name == null || this.name.isEmpty()) {
                throw new JobSchedulerException("No scheduler name was specified");
            }

            if (this.instanceId == null || this.instanceId.isEmpty()) {
                throw new JobSchedulerException("No scheduler instanceId was specified");
            }

            if (this.threadPoolSize <= 0) {
                throw new JobSchedulerException("Invalid thread pool size specified (<= 0)");
            }

            try {
                DirectSchedulerFactory.getInstance().createScheduler(this.name,this.instanceId,createThreadPool(),createJobStore());
                Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler(this.name);
                return new QuartzJobScheduler(scheduler);
            }catch(SchedulerException e) {
                throw new JobSchedulerException("Could not create quartz scheduler",e);
            }
        }

        private JobStore createJobStore() {

            return new RAMJobStore();
        }

        private ThreadPool createThreadPool() {

            return new SimpleThreadPool(this.threadPoolSize,Thread.NORM_PRIORITY);
        }
    }
}

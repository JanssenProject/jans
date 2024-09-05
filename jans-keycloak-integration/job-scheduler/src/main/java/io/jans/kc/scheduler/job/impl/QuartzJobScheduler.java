package io.jans.kc.scheduler.job.impl;


import io.jans.kc.scheduler.job.RecurringJobSpec;
import io.jans.kc.scheduler.job.JobScheduler;
import io.jans.kc.scheduler.job.JobSchedulerException;

import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

import org.quartz.JobDataMap;
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
import org.quartz.impl.StdSchedulerFactory;

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
            
            Trigger trigger = createRecurringJobTrigger(spec);
        
            JobDetail jobdetail = newJob(QuartzJobWrapper.class)
                    .withIdentity(spec.getName())
                    .usingJobData(QuartzJobWrapper.JOB_NAME_ENTRY_KEY,spec.getName())
                    .usingJobData(QuartzJobWrapper.JOB_CLASS_ENTRY_KEY,spec.getJobClass().getName())
                    .build();
            
            quartzScheduler.scheduleJob(jobdetail,trigger);
            return this;
        }catch(SchedulerException e) {
            throw new JobSchedulerException("Unable to schedule recurring job",e);
        }
    }

    private Trigger createRecurringJobTrigger(RecurringJobSpec spec) {

        Trigger trigger = null;
        int interval = Math.toIntExact(spec.schedulingInterval().toSeconds());
        int count = spec.repeatCount();

        if(spec.repeatForever()) {
            trigger = newTrigger()
                .withIdentity(recurringJobTriggerKey(spec))
                .withSchedule(simpleSchedule().repeatSecondlyForever(interval))
                .startNow()
                .build();
        }else {
            trigger = newTrigger()
                .withIdentity(recurringJobTriggerKey(spec))
                .withSchedule(simpleSchedule().repeatSecondlyForTotalCount(count,interval))
                .startNow()
                .build();
        }
        return trigger;
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
                StdSchedulerFactory schedulerfactory = new StdSchedulerFactory();
                schedulerfactory.initialize(schedulerProperties());
                Scheduler scheduler = schedulerfactory.getScheduler();
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

        private Properties schedulerProperties() {

            Properties props = new Properties();
            props.setProperty("org.quartz.scheduler.instanceName",this.name);
            props.setProperty("org.quartz.scheduler.instanceId",this.instanceId);
            props.setProperty("org.quartz.scheduler.threadsInheritContextClassLoaderOfInitializer","true");
            props.setProperty("org.quartz.threadPool.class",SimpleThreadPool.class.getName());
            props.setProperty("org.quartz.threadPool.threadCount",Integer.toString(this.threadPoolSize));
            props.setProperty("org.quartz.threadPool.threadPriority",Integer.toString(Thread.NORM_PRIORITY));
            props.setProperty("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread","true");
            props.setProperty("org.quartz.jobStore.class","org.quartz.simpl.RAMJobStore");
            return props;
        }
    }
}

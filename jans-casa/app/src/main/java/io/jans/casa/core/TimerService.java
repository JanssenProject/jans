package io.jans.casa.core;

import io.jans.casa.misc.DumbQuartzJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.util.Date;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * @author jgomer
 */
@Dependent
public class TimerService {

    @Inject
    private Logger logger;

    private String group = getClass().getSimpleName();

    private Scheduler scheduler;

    @PostConstruct
    private void inited() {

        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            if (!scheduler.isStarted()) {
                scheduler.start();
            }
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
        }

    }

    public JobKey schedule(String name, int count, int sleepTime) throws SchedulerException {
        return schedule(name, 0, count - 1, sleepTime);
    }

    public JobKey schedule(String name, int gap, int count, int sleepTime) throws SchedulerException {

        JobDetail job = JobBuilder.newJob(DumbQuartzJob.class).withIdentity(name, group).build();

        SimpleScheduleBuilder builder = simpleSchedule().withIntervalInSeconds(sleepTime);
        builder = count < 0 ?  builder.repeatForever() : builder.withRepeatCount(count);
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger_" + name, group)
                .startAt(new Date(System.currentTimeMillis() + gap * 1000)).withSchedule(builder).build();

        scheduler.scheduleJob(job, trigger);
        return job.getKey();

    }

    public void addListener(JobListener jobListener, String jobName) throws SchedulerException {
        scheduler.getListenerManager().addJobListener(jobListener, KeyMatcher.keyEquals(new JobKey(jobName, group)));
    }

    public boolean cancel(JobKey jobKey) {

        try {
            logger.debug("Cancelling job {}", jobKey.toString());
            return scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }

}

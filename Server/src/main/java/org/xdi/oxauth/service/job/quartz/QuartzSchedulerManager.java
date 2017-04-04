package org.xdi.oxauth.service.job.quartz;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.slf4j.Logger;
import org.xdi.util.init.Initializable;

/**
 * @author Yuriy Movchan
 * Date: 04/04/2017
 */
@ApplicationScoped
public class QuartzSchedulerManager extends Initializable {

	public static final String QUARTZ_PROPERTY_FILE_NAME = "quartz.properties";

	@Inject
	private Logger log;

	@Inject
	private RequestJobListener jobListener;

	@Inject
	private JobExecutionFactory jobFactory;

	private Scheduler scheduler;

	protected void initInternal() {
		try {
			StdSchedulerFactory factory = new StdSchedulerFactory();
			factory.initialize(QUARTZ_PROPERTY_FILE_NAME);

			scheduler = factory.getScheduler();

			// Register job listener to bound request context to every job execution
			scheduler.getListenerManager().addJobListener(jobListener, EverythingMatcher.allJobs());
			// Replace default job factory
			scheduler.setJobFactory(jobFactory);
		} catch (SchedulerException ex) {
			throw new IllegalStateException("Failed to initialize Quartz scheduler manager", ex);
		}

		log.info("Quartz scheduler manager initialized");
	}

	public void schedule(JobShedule jobShedule) {
		schedule(jobShedule.getJobDetail(), jobShedule.getTrigger());
	}

	public void schedule(JobDetail jobDetail, Trigger trigger) {
		checkInitialized();

		try {
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException ex) {
			throw new IllegalStateException("Failed to shedule Quartz job", ex);
		}
	}

	public void start() {
		if (!isInitialized())
			super.init();

		checkInitialized();

		try {
			scheduler.start();
		} catch (SchedulerException ex) {
			throw new IllegalStateException("Failed to start Quartz sheduler", ex);
		}

		log.info("Quartz scheduler started");
	}

	public void standby() {
		checkInitialized();

		try {
			scheduler.standby();
		} catch (SchedulerException ex) {
			throw new IllegalStateException("Failed to set Quartz sheduler stand-by mode", ex);
		}

		log.info("Quartz scheduler in stand-by mode");
	}

	public void clear() {
		checkInitialized();

		try {
			scheduler.clear();
		} catch (SchedulerException ex) {
			throw new IllegalStateException("Failed to clear Quartz sheduler", ex);
		}

		log.info("Quartz scheduler is clear now");
	}

	@PreDestroy
	public void destroy() {
		if (!isInitialized())
			return;

		try {
			scheduler.shutdown();
			log.info("Quartz scheduler manager destroyed");
		} catch (SchedulerException ex) {
			log.warn("Cannot shutdown quartz scheduler!", ex);
		}
	}

	public boolean isInitialized() {
		return scheduler != null;
	}

	private void checkInitialized() {
		if (!isInitialized())
			throw new IllegalStateException("Quartz scheduler manager not initialized!");
	}

}

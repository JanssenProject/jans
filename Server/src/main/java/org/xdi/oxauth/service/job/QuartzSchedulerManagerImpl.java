package org.xdi.oxauth.service.job;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class QuartzSchedulerManagerImpl implements QuartzSchedulerManager {

	private static final Logger logger = LoggerFactory
			.getLogger(QuartzSchedulerManagerImpl.class);

	private static final String ASYNC_GROUP = "CdiqiAsync";

	@Inject
	private BoundRequestJobListener jobListener;

	@Inject
	private CdiJobFactory jobFactory;

	private Scheduler scheduler;

	/*
	 * (non-Javadoc)
	 * 
	 * @see cz.symbiont_it.wqi.QuartzSchedulerManager#init()
	 */
	@SuppressWarnings("unchecked")
	public void init() throws SchedulerException {

		if (isInitialized())
			return;

		StdSchedulerFactory factory = new StdSchedulerFactory();
		factory.initialize(QUARTZ_PROPERTY_FILE_NAME);

		scheduler = factory.getScheduler();

		// Register job listener to bound request context to every job execution
		scheduler.getListenerManager().addJobListener(jobListener,
				EverythingMatcher.allJobs());
		// Replace default job factory
		scheduler.setJobFactory(jobFactory);

		logger.info("Quartz scheduler manager initialized");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cz.symbiont_it.wqi.QuartzSchedulerManager#schedule(org.quartz.JobDetail,
	 * org.quartz.Trigger)
	 */
	public void schedule(JobDetail jobDetail, Trigger trigger)
			throws SchedulerException {
		checkInitialized();
		scheduler.scheduleJob(jobDetail, trigger);
	}

	/**
	 * 
	 * @param asyncEvent
	 * @throws SchedulerException
	 */
	public void observeAndFireAsyncEvent(@Observes AsyncEvent asyncEvent)
			throws SchedulerException {

		checkRunning();

		JobDataMap dataMap = new JobDataMap();
		dataMap.put(AsyncJob.KEY_ASYNC_EVENT, asyncEvent);
		String id = UUID.randomUUID().toString();

		JobDetail asyncJob = newJob(AsyncJob.class)
				.withIdentity(AsyncJob.class.getSimpleName() + "_" + id,
						ASYNC_GROUP).usingJobData(dataMap).build();
		Trigger asyncTrigger = newTrigger().withIdentity(id, ASYNC_GROUP)
				.startNow().withSchedule(simpleSchedule()).build();
		schedule(asyncJob, asyncTrigger);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cz.symbiont_it.wqi.QuartzSchedulerManager#start()
	 */
	public void start() throws SchedulerException {

		if (!isInitialized())
			init();

		scheduler.start();
		logger.info("Quartz scheduler started");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cz.symbiont_it.wqi.QuartzSchedulerManager#standby()
	 */
	public void standby() throws SchedulerException {
		checkInitialized();
		scheduler.standby();
		logger.info("Quartz scheduler in stand-by mode");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cz.symbiont_it.wqi.api.QuartzSchedulerManager#clear()
	 */
	@Override
	public void clear() throws SchedulerException {
		checkInitialized();
		scheduler.clear();
		logger.info("Quartz scheduler is clear now");
	}

	@PreDestroy
	public void destroy() {

		if (!isInitialized())
			return;

		try {
			scheduler.shutdown();
			logger.info("Quartz scheduler manager destroyed");
		} catch (SchedulerException e) {
			logger.warn("Cannot shutdown quartz scheduler!");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cz.symbiont_it.wqi.QuartzSchedulerManager#isInitialized()
	 */
	public boolean isInitialized() {
		return scheduler != null;
	}

	private void checkInitialized() {
		if (!isInitialized())
			throw new IllegalStateException(
					"Quartz scheduler manager not initialized");
	}

	private void checkRunning() throws SchedulerException {
		if (!isInitialized() || !scheduler.isStarted()
				|| scheduler.isInStandbyMode())
			throw new IllegalStateException("Scheduler not running");
	}

}

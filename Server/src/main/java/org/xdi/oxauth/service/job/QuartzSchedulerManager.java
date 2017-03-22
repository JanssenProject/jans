package org.xdi.oxauth.service.job;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public interface QuartzSchedulerManager {

	public static final String QUARTZ_PROPERTY_FILE_NAME = "cdiqi-quartz.properties";
	
	/**
	 * Perform init.
	 * 
	 * @throws SchedulerException
	 */
	public void init() throws SchedulerException;

	/**
	 * @return <code>true</code> if initialized, <code>false</code> otherwise
	 */
	public boolean isInitialized();

	/**
	 * Start underlying quartz scheduler. If not initialized perform init first.
	 * 
	 * @throws SchedulerException
	 */
	public void start() throws SchedulerException;

	/**
	 * Pause underlying quartz scheduler.
	 * 
	 * @throws SchedulerException
	 */
	public void standby() throws SchedulerException;

	/**
	 * Deletes all scheduling data.
	 * 
	 * @throws SchedulerException
	 */
	public void clear() throws SchedulerException;

	/**
	 * Schedule new job.
	 * 
	 * @param jobDetail
	 * @param trigger
	 * @throws SchedulerException
	 */
	public void schedule(JobDetail jobDetail, Trigger trigger)
			throws SchedulerException;

}

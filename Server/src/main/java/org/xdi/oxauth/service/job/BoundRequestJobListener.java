package org.xdi.oxauth.service.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public abstract class BoundRequestJobListener implements JobListener {

	protected static final String REQUEST_DATA_STORE_KEY = BoundRequestJobListener.class
			.getName() + "_REQUEST_DATA_STORE_KEY";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quartz.JobListener#getName()
	 */
	public String getName() {
		return getClass().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.quartz.JobListener#jobToBeExecuted(org.quartz.JobExecutionContext)
	 */
	public void jobToBeExecuted(JobExecutionContext context) {
		startRequest(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.quartz.JobListener#jobExecutionVetoed(org.quartz.JobExecutionContext)
	 */
	public void jobExecutionVetoed(JobExecutionContext context) {
		endRequest(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.quartz.JobListener#jobWasExecuted(org.quartz.JobExecutionContext,
	 * org.quartz.JobExecutionException)
	 */
	public void jobWasExecuted(JobExecutionContext context,
			JobExecutionException jobException) {
		endRequest(context);
	}

	/**
	 * @param context
	 */
	protected abstract void startRequest(JobExecutionContext context);

	/**
	 * @param context
	 */
	protected abstract void endRequest(JobExecutionContext context);

}

package org.xdi.oxauth.service.job;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncJob implements Job {

	public static final String KEY_ASYNC_EVENT = AsyncEvent.class.getName();

	private static final Logger logger = LoggerFactory
			.getLogger(AsyncJob.class);

	@Inject
	private BeanManager beanManager;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {

		try {

			AsyncEvent asyncEvent = (AsyncEvent) context.getJobDetail()
					.getJobDataMap().remove(KEY_ASYNC_EVENT);

			if (asyncEvent == null)
				// No async event to process - should never happen
				return;

			logger.debug("Fire async event [{}] with qualifiers {}", asyncEvent
					.getTargetEvent().getClass().getName(),
					asyncEvent.getQualifiers());

			beanManager.fireEvent(asyncEvent.getTargetEvent(),
					asyncEvent.getQualifiers());

		} catch (Exception e) {
			throw new JobExecutionException(e);
		}
	}

}

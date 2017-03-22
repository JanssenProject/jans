package org.xdi.oxauth.service.job;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@RequestScoped
public class JobExecutionDelegate implements Job {

	@Inject
	private Instance<Job> jobInstance;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {

		Instance<? extends Job> job = jobInstance.select(context
				.getJobDetail().getJobClass());

		if (job.isAmbiguous())
			throw new IllegalStateException(
					"Cannot produce job: ambiguous instance");

		if (job.isUnsatisfied())
			throw new IllegalStateException(
					"Cannot produce job: unsatisfied instance");

		job.get().execute(context);
	}

}

package org.xdi.oxauth.service.job;

import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * In order to avoid memory leaks with <tt>@Dependent</tt> jobs (see <a
 * href="https://issues.jboss.org/browse/WELD-920">WELD-920</a>) this factory
 * does not "produce" jobs directly.
 * 
 * It produces request scoped delegate. In fact it does not produce the real
 * instance but uninitialized proxy instead (see <a
 * href="https://issues.jboss.org/browse/CDI-125">CDI-125</a>). Unless CDI
 * implementations work this way this factory will not work correctly.
 */
public class CdiJobFactory implements JobFactory {

	@Inject
	private JobExecutionDelegate jobExecutionDelegate;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.quartz.spi.JobFactory#newJob(org.quartz.spi.TriggerFiredBundle,
	 * org.quartz.Scheduler)
	 */
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler)
			throws SchedulerException {
		return jobExecutionDelegate;
	}

}

package org.xdi.oxauth.service.job;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

@ApplicationScoped
public class CdiJobFactory2 implements JobFactory {

	@Inject
	private BeanManager beanManager;

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
		Class<? extends Job> jobClazz = bundle.getJobDetail().getJobClass();
		Bean<?> bean = beanManager.getBeans(jobClazz).iterator().next();
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		Object object = beanManager.getReference(bean, jobClazz, ctx);

		return (Job) object;
	}

}

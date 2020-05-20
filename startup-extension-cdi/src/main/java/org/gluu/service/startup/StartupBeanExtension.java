package org.gluu.service.startup;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

public class StartupBeanExtension implements Extension {

	private final Set<Bean<?>> startupBeans = new LinkedHashSet<Bean<?>>();

	<X> void processBean(@Observes ProcessBean<X> event) {
		if (event.getAnnotated().isAnnotationPresent(Startup.class) && event.getAnnotated().isAnnotationPresent(ApplicationScoped.class)) {
			startupBeans.add(event.getBean());
		}
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager manager) {
		for (Bean<?> bean : startupBeans) {
			// the call to toString() is a cheat to force the bean to be initialized
			manager.getReference(bean, bean.getBeanClass(), manager.createCreationalContext(bean)).toString();
		}
	}

}
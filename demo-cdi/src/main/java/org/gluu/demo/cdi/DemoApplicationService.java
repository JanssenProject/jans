package org.gluu.demo.cdi;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

/**
 * Demo application service
 *
 * @author Yuriy Movchan Date: 08/14/2019
 */
@ApplicationScoped
@Named
public class DemoApplicationService {

	@Inject
	private Logger log;

	@PostConstruct
	public void createApplicationComponents() {
		log.info("Created Demo application service");
	}

	public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
		log.info("Initializing Demo application service");
	}

}

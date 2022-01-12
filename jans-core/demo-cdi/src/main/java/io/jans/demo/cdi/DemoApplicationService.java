/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.demo.cdi;

import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;

/**
 * Demo application service
 *
 * @author Yuriy Movchan Date: 08/14/2019
 */
@ApplicationScoped
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
	
	@Produces
	@ApplicationScoped
	@Named("ext_msgs")
	public HashMap<String, String> buildExternalMessagesMap() {
		HashMap<String, String> result = new HashMap<String, String>();
		
		// It's for demo to show how to outject Map
		result.put("dummy_message_1", "Dummy message 1");
		result.put("dummy_message_2", "Dummy message 2");
		result.put("dummy_message_3", "Dummy message 3");
		
		return result;
	}

}

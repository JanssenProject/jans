/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.service;

import org.slf4j.Logger;

import io.jans.util.security.SecurityProviderUtility;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Initialize application level beans
 *
 * @author Yuriy Movchan Date: 06/24/2017
 */
@ApplicationScoped
@Named
public class AppInitializer {

	@Inject
	private Logger log;

	@PostConstruct
    public void createApplicationComponents() {
		SecurityProviderUtility.installBCProvider();
    }

	// Don't remove this. It force CDI to create bean at startup
	public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
		log.info("Application initialized");
    }

}

/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author Yuriy Movchan
 * @version September 13, 2017
 */
@ApplicationScoped
@Named
public class AppInitializer {
	
	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private ApplicationService applicationService;

	@PostConstruct
	public void createApplicationComponents() {
	}

	public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
        configurationFactory.create();
        applicationService.init();
	}

}
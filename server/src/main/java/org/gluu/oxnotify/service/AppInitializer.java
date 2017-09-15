/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.oxnotify.service;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Yuriy Movchan
 * @version September 13, 2017
 */
@ApplicationScoped
@Named
public class AppInitializer {
	
	@Inject
	private ConfigurationFactory configurationFactory;

	@PostConstruct
	public void createApplicationComponents() {
	}

	public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
        configurationFactory.create();
	}

}
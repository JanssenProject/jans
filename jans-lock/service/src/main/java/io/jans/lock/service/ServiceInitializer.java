/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service;

import org.slf4j.Logger;

import io.jans.lock.service.config.ConfigurationFactory;
import io.jans.lock.service.message.TokenSubService;
import io.jans.lock.service.policy.PolicyDownloadService;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * 
 * Lock services initializer
 *
 * @author Yuriy Movchan Date: 12/18/2023
 */
@ApplicationScoped
public class ServiceInitializer {

	@Inject
	private Logger log;

	@Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private TokenSubService tokenSubService;

    @Inject
    private PolicyDownloadService policyDownloadService;

	public void applicationInitialized(@Observes ApplicationInitializedEvent applicationInitializedEvent) {
		log.info("Initializing Lock service module services");

		configurationFactory.initTimer();
		tokenSubService.subscribe();
		policyDownloadService.initTimer();

		log.debug("Initializing Lock service module services complete");
	}

}

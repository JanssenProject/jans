/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.security.Provider;
import java.security.Security;

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
    	installBCProvider();
    }

	// Don't remove this. It force CDI to create bean at startup
	public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
		log.info("Application initialized");
    }

	private void installBCProvider(boolean silent) {
		Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
		if (provider == null) {
			if (!silent) {
				log.info("Adding Bouncy Castle Provider");
			}

			Security.addProvider(new BouncyCastleProvider());
		} else {
			if (!silent) {
				log.info("Bouncy Castle Provider was added already");
			}
		}
	}

	private void installBCProvider() {
		installBCProvider(false);
	}

}

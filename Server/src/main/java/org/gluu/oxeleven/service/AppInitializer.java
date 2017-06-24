package org.gluu.oxeleven.service;

import java.security.Provider;
import java.security.Security;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;

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

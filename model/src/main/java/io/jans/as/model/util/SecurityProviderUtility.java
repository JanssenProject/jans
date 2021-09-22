/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class SecurityProviderUtility {

	private static final Logger log = Logger.getLogger(SecurityProviderUtility.class);

	private static BouncyCastleProvider bouncyCastleProvider;

	private SecurityProviderUtility() {
    }

	public static void installBCProvider(boolean silent) {
		bouncyCastleProvider = (BouncyCastleProvider) Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
		if (bouncyCastleProvider == null) {
			if (!silent) {
				log.info("Adding Bouncy Castle Provider");
			}

			bouncyCastleProvider = new BouncyCastleProvider();
			Security.addProvider(bouncyCastleProvider);
		} else {
			if (!silent) {
				log.info("Bouncy Castle Provider was added already");
			}
		}
	}

	public static void installBCProvider() {
		installBCProvider(false);
	}
	
	public static BouncyCastleProvider getInstance() {
	    return bouncyCastleProvider;
	}
}

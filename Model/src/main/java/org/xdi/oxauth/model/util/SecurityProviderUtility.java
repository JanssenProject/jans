/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

import java.security.Provider;
import java.security.Security;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SecurityProviderUtility {

	private static final Logger log = Logger.getLogger(JwtUtil.class);

	public static void installBCProvider(boolean silent) {
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

	public static void installBCProvider() {
		installBCProvider(false);
	}

}

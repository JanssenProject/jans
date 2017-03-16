/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xdi.model.SmtpConfiguration;
import org.xdi.oxauth.crypto.random.RandomChallengeGenerator;
import org.xdi.oxauth.crypto.signature.SHA256withECDSASignatureVerification;
import org.xdi.oxauth.model.appliance.GluuAppliance;
import org.xdi.service.cache.CacheConfiguration;
import org.xdi.service.cache.InMemoryConfiguration;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter.EncryptionException;

/**
 * Holds factory methods to create services
 *
 * @author Yuriy Movchan Date: 05/22/2015
 */
public class ApplicationFactory {
    
    @Inject
    private ApplianceService applianceService;
    
    @Inject
    private EncryptionService encryptionService;

    @Inject
    private Logger log;

    @Produces @ApplicationScoped
    public RandomChallengeGenerator getRandomChallengeGenerator() {
        return new RandomChallengeGenerator();
    }

    @Produces @ApplicationScoped @Named("sha256withECDSASignatureVerification")
    public SHA256withECDSASignatureVerification getBouncyCastleSignatureVerification() {
        return new SHA256withECDSASignatureVerification();
    }

	@Produces @ApplicationScoped
	public CacheConfiguration getCacheConfiguration() {
		CacheConfiguration cacheConfiguration = applianceService.getAppliance().getCacheConfiguration();
		if (cacheConfiguration == null || cacheConfiguration.getCacheProviderType() == null) {
			log.error("Failed to read cache configuration from LDAP. Please check appliance oxCacheConfiguration attribute " +
					"that must contain cache configuration JSON represented by CacheConfiguration.class. Applieance DN: " + applianceService.getAppliance().getDn());
			log.info("Creating fallback IN-MEMORY cache configuration ... ");

			cacheConfiguration = new CacheConfiguration();
			cacheConfiguration.setInMemoryConfiguration(new InMemoryConfiguration());

			log.info("IN-MEMORY cache configuration is created.");
		}
		log.info("Cache configuration: " + cacheConfiguration);
		return cacheConfiguration;
	}

	@Produces @ApplicationScoped
	public SmtpConfiguration getSmtpConfiguration() {
		GluuAppliance appliance = applianceService.getAppliance();
		SmtpConfiguration smtpConfiguration = appliance.getSmtpConfiguration();
		
		if (smtpConfiguration == null) {
			return null;
		}

		String password = smtpConfiguration.getPassword();
		if (StringHelper.isNotEmpty(password)) {
			try {
				smtpConfiguration.setPasswordDecrypted(encryptionService.decrypt(password));
			} catch (EncryptionException ex) {
				log.error("Failed to decript SMTP user password", ex);
			}
		}
		
		return smtpConfiguration;
	}

}
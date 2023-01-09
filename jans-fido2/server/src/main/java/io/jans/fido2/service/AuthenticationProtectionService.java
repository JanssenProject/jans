/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service;

import io.jans.as.model.configuration.AuthenticationProtectionConfiguration;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.fido2.model.conf.AppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Brute Force authentication protection service implementation
 *
 * @author Yuriy Movchan Date: 08/21/2018
 */
@ApplicationScoped
@Named
public class AuthenticationProtectionService extends io.jans.service.security.protect.AuthenticationProtectionService {

    private static final int DEFAULT_ATTEMPT_EXPIRATION = 15; // 15 seconds

    private static final int DEFAULT_MAXIMUM_ALLOWED_ATTEMPTS_WITHOUT_DELAY = 4; // 4 attempts

    private static final int DEFAULT_DELAY_TIME = 2; // 5 seconds

    private static final String DEFAULT_KEY_PREFIX = "user";

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    protected void init() {
        updateConfiguration(appConfiguration);
    }

    public void updateConfiguration(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
        AuthenticationProtectionConfiguration authenticationProtectionConfiguration = appConfiguration.getAuthenticationProtectionConfiguration();
        if (authenticationProtectionConfiguration == null) {
            this.attemptExpiration = DEFAULT_ATTEMPT_EXPIRATION;
            this.maximumAllowedAttemptsWithoutDelay = DEFAULT_MAXIMUM_ALLOWED_ATTEMPTS_WITHOUT_DELAY;

            this.delayTime = DEFAULT_DELAY_TIME;
        } else {
            this.attemptExpiration = authenticationProtectionConfiguration.getAttemptExpiration();
            this.maximumAllowedAttemptsWithoutDelay = authenticationProtectionConfiguration.getMaximumAllowedAttemptsWithoutDelay();

            this.delayTime = authenticationProtectionConfiguration.getDelayTime();
        }
    }

    @Override
    protected String getKeyPrefix() {
        return DEFAULT_KEY_PREFIX;
    }

    public boolean isEnabled() {
        AuthenticationProtectionConfiguration authenticationProtectionConfiguration = appConfiguration.getAuthenticationProtectionConfiguration();

        return (authenticationProtectionConfiguration != null) && (authenticationProtectionConfiguration.getBruteForceProtectionEnabled());

    }

}

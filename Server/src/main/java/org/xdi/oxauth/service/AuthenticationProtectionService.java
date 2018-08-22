package org.xdi.oxauth.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.configuration.AuthenticationProtectionConfiguration;

/**
 * Brute Force authentication protection service implementation
 *
 * @author Yuriy Movchan Date: 08/21/2018
 */
@ApplicationScoped
@Named
public class AuthenticationProtectionService extends org.xdi.service.security.protect.AuthenticationProtectionService {

    private static final int DEFAULT_ATTEMPT_EXPIRATION = 15; // 15 seconds

    private static final int DEFAULT_MAXIMUM_ALLOWED_ATTEMPTS = 10; // 10 attempts
    private static final int DEFAULT_MAXIMUM_ALLOWED_ATTEMPTS_WITHOUT_DELAY = 4; // 4 attempts

    private static final int DEFAULT_DELAY_TIME = 2; // 5 seconds

    private static final String DEFAULT_KEY_PREFIX = "user";
    
    @Inject
    private AppConfiguration appConfiguration;

    @Override
    protected void init() {
        AuthenticationProtectionConfiguration authenticationProtectionConfiguration = appConfiguration.getAuthenticationProtectionConfiguration();
        if (authenticationProtectionConfiguration == null) {
            this.attemptExpiration = DEFAULT_ATTEMPT_EXPIRATION;
            this.maximumAllowedAttempts = DEFAULT_MAXIMUM_ALLOWED_ATTEMPTS;
            this.maximumAllowedAttemptsWithoutDelay = DEFAULT_MAXIMUM_ALLOWED_ATTEMPTS_WITHOUT_DELAY;

            this.delayTime = DEFAULT_DELAY_TIME;
        } else {
            this.attemptExpiration = authenticationProtectionConfiguration.getAttemptExpiration();
            this.maximumAllowedAttempts = authenticationProtectionConfiguration.getMaximumAllowedAttempts();
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

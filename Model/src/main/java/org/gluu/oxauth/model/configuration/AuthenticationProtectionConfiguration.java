package org.gluu.oxauth.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Brute Force authentication configuration
 *
 * @author Yuriy Movchan Date: 08/22/2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationProtectionConfiguration {

    private int attemptExpiration;
    private int maximumAllowedAttemptsWithoutDelay;

    private int delayTime;
    
    private Boolean bruteForceProtectionEnabled;

    public final int getAttemptExpiration() {
        return attemptExpiration;
    }

    public final void setAttemptExpiration(int attemptExpiration) {
        this.attemptExpiration = attemptExpiration;
    }

    public final int getMaximumAllowedAttemptsWithoutDelay() {
        return maximumAllowedAttemptsWithoutDelay;
    }

    public final void setMaximumAllowedAttemptsWithoutDelay(int maximumAllowedAttemptsWithoutDelay) {
        this.maximumAllowedAttemptsWithoutDelay = maximumAllowedAttemptsWithoutDelay;
    }

    public final int getDelayTime() {
        return delayTime;
    }

    public final void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    public final Boolean getBruteForceProtectionEnabled() {
        return bruteForceProtectionEnabled;
    }

    public final void setBruteForceProtectionEnabled(Boolean bruteForceProtectionEnabled) {
        this.bruteForceProtectionEnabled = bruteForceProtectionEnabled;
    }

}

package org.xdi.oxauth.model.configuration;

/**
 * Brute Force authentication configuration
 *
 * @author Yuriy Movchan Date: 08/22/2018
 */
public class AuthenticationProtectionConfiguration {

    private int attemptExpiration;
    private int maximumAllowedAttempts;
    private int maximumAllowedAttemptsWithoutDelay;

    private int delayTime;
    
    private Boolean bruteForceProtectionEnabled;

    public final int getAttemptExpiration() {
        return attemptExpiration;
    }

    public final void setAttemptExpiration(int attemptExpiration) {
        this.attemptExpiration = attemptExpiration;
    }

    public final int getMaximumAllowedAttempts() {
        return maximumAllowedAttempts;
    }

    public final void setMaximumAllowedAttempts(int maximumAllowedAttempts) {
        this.maximumAllowedAttempts = maximumAllowedAttempts;
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

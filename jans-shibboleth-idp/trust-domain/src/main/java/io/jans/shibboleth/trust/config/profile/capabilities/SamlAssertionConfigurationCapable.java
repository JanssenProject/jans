package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;

import java.time.Duration;

public interface SamlAssertionConfigurationCapable {

    public AssertionTimeCondition getAssertionTimeCondition();
    public Duration getAssertionLifetime();
    public AssertionSigningPolicy getAssertionSigningPolicy();
}
package io.jans.shibboleth.model.config.profiles.capabilities;

import io.jans.shibboleth.model.config.profiles.common.AssertionTimeCondition;
import io.jans.shibboleth.model.config.profiles.common.AssertionSigningPolicy;

import java.time.Duration;

public interface SamlAssertionConfigurationCapable {

    public AssertionTimeCondition getAssertionTimeCondition();
    public Duration getAssertionLifetime();
    public AssertionSigningPolicy getAssertionSigningPolicy();
}
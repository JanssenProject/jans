package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;

import java.time.Duration;

public interface SamlAssertionConfigurationCapable {

    SamlAssertionConfigurationSupport samlAssertionConfigurationSupport();

    default AssertionTimeCondition getAssertionTimeCondition() {

        return samlAssertionConfigurationSupport().assertionTimeCondition();
    }

    default Duration getAssertionLifetime() {

        return samlAssertionConfigurationSupport().assertionLifetime();
    }

    default AssertionSigningPolicy getAssertionSigningPolicy() {

        return samlAssertionConfigurationSupport().assertionSigningPolicy();
    }
}

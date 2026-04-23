package io.jans.shibboleth.model.config.profiles.support;

import java.time.Duration;

import io.jans.shibboleth.model.config.profiles.common.AssertionSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionTimeCondition;

public class SamlAssertionConfigurationSupport {

    private static final Duration DEFAULT_ASSERTION_LIFETIME = Duration.ofMinutes(5);

    private final AssertionSigningPolicy assertionSigningPolicy;
    private final AssertionTimeCondition assertionTimeCondition;
    private final Duration assertionLifetime;

    private SamlAssertionConfigurationSupport(AssertionSigningPolicy assertionSigningPolicy, AssertionTimeCondition assertionTimeCondition,Duration assertionLifetime) {

        this.assertionSigningPolicy = assertionSigningPolicy != null ? assertionSigningPolicy : AssertionSigningPolicy.SIGN_ASSERTIONS;
        this.assertionTimeCondition = assertionTimeCondition != null ? assertionTimeCondition : AssertionTimeCondition.INCLUDE_NOT_BEFORE;
        this.assertionLifetime = assertionLifetime != null ? assertionLifetime : DEFAULT_ASSERTION_LIFETIME;
    }

    public static SamlAssertionConfigurationSupport of(AssertionSigningPolicy assertionSigningPolicy, AssertionTimeCondition assertionTimeCondition, Duration assertionLifetime) {

        return new SamlAssertionConfigurationSupport(assertionSigningPolicy,assertionTimeCondition,assertionLifetime);
    }

    public AssertionSigningPolicy getAssertionSigningPolicy() {

        return assertionSigningPolicy;
    }

    public AssertionTimeCondition getAssertionTimeCondition() {

        return assertionTimeCondition;
    }

    public Duration getAssertionLifetime() {

        return assertionLifetime;
    }
}
package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;

import java.time.Duration;

@SuppressWarnings("unchecked")
interface SamlAssertionBuilderSteps<B extends SamlAssertionBuilderSteps<B>> {

    SamlAssertionConfigurationSupport.Builder samlAssertionBuilder();

    default B assertionSigningPolicy(AssertionSigningPolicy policy) {

        samlAssertionBuilder().assertionSigningPolicy(policy);
        return (B) this;
    }

    default B assertionTimeCondition(AssertionTimeCondition condition) {

        samlAssertionBuilder().assertionTimeCondition(condition);
        return (B) this;
    }

    default B assertionLifetime(Duration lifetime) {

        samlAssertionBuilder().assertionLifetime(lifetime);
        return (B) this;
    }
}

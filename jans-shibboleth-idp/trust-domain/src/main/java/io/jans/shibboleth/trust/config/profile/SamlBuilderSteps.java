package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;

@SuppressWarnings("unchecked")
interface SamlBuilderSteps<B extends SamlBuilderSteps<B>> {

    SamlConfigurationSupport.Builder samlBuilder();

    default B messageSigningPolicy(MessageSigningPolicy policy) {

        samlBuilder().messageSigningPolicy(policy);
        return (B) this;
    }
}

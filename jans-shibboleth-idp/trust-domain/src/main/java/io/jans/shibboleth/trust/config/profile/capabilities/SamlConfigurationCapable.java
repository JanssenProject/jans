package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;

public interface SamlConfigurationCapable {

    public MessageSigningPolicy getMessageSigningPolicy();
}
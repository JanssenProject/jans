package io.jans.shibboleth.model.config.profiles.capabilities;

import io.jans.shibboleth.model.config.profiles.common.MessageSigningPolicy;

public interface SamlConfigurationCapable {

    public MessageSigningPolicy getMessageSigningPolicy();
}
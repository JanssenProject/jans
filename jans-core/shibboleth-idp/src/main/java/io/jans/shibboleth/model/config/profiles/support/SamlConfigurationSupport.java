package io.jans.shibboleth.model.config.profiles.support;

import io.jans.shibboleth.model.config.profiles.common.MessageSigningPolicy;

public class SamlConfigurationSupport {

    private final MessageSigningPolicy messageSigningPolicy;

    private SamlConfigurationSupport(MessageSigningPolicy messageSigningPolicy) {

        this.messageSigningPolicy = messageSigningPolicy != null ? messageSigningPolicy : MessageSigningPolicy.SIGN_BOTH;
    }

    public static SamlConfigurationSupport of(MessageSigningPolicy messageSigningPolicy) {

        return new SamlConfigurationSupport(messageSigningPolicy);
    }

    public static SamlConfigurationSupport of() {

        return new SamlConfigurationSupport(null);
    }

    public MessageSigningPolicy getMessageSigningPolicy() {

        return messageSigningPolicy;
    }
}
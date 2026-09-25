package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;

public interface SamlConfigurationCapable {

    SamlConfigurationSupport samlConfigurationSupport();

    default MessageSigningPolicy getMessageSigningPolicy() {

        return samlConfigurationSupport().messageSigningPolicy();
    }
}

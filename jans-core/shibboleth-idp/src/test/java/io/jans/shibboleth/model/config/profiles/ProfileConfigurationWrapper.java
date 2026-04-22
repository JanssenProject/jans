package io.jans.shibboleth.model.config.profiles;

import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.config.profiles.capabilities.AuthenticationConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.Saml2SsoConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.SamlAssertionConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.SamlConfigurationCapable;

public class ProfileConfigurationWrapper {

    private final Object config;
    private final ProfileType type;

    public ProfileConfigurationWrapper(Object config, ProfileType type) {

        this.config = config;
        this.type = type;
    }

    public Object getProfileConfig() {

        return config;
    }

    public ProfileType getType() {

        return type;
    }

    public boolean hasProfileConfigurationCapabilities(Class<?> capabilitiesclazz) {

        return capabilitiesclazz.isInstance(config);
    }

    public CommonConfigurationCapable asCommonConfigurationCapable() {

        return (CommonConfigurationCapable) config;
    }

    public AuthenticationConfigurationCapable asAuthenticationConfigurationCapable() {
        
        return (AuthenticationConfigurationCapable) config;
    }

    public SamlAssertionConfigurationCapable asSamlAssertionConfigurationCapable() {

        return (SamlAssertionConfigurationCapable) config;
    }

    public SamlConfigurationCapable asSamlConfigurationCapable() {

        return (SamlConfigurationCapable) config;
    }

    public Saml2ConfigurationCapable asSaml2ConfigurationCapable() {

        return (Saml2ConfigurationCapable) config;
    }

    public Saml2SsoConfigurationCapable asSaml2SsoConfigurationCapable() {

        return (Saml2SsoConfigurationCapable) config;
    }
}
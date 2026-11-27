package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.support.AuthenticationConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;

interface ProfileConfigurationAccess {

    default CommonConfigurationSupport commonConfigurationSupport() {

        return null;
    }

    default AuthenticationConfigurationSupport authenticationConfigurationSupport() {

        return null;
    }

    default SamlConfigurationSupport samlConfigurationSupport() {

        return null;
    }

    default Saml2ConfigurationSupport saml2ConfigurationSupport() {

        return null;
    }

    default SamlAssertionConfigurationSupport samlAssertionConfigurationSupport() {

        return null;
    }

    default Saml2SsoConfigurationSupport saml2SsoConfigurationSupport() {

        return null;
    }
}

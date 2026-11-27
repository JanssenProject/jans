package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.support.AuthenticationConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;

abstract class AbstractBuilderSteps<B extends AbstractBuilderSteps<B>> {

    private final CommonConfigurationSupport.Builder common;
    private final AuthenticationConfigurationSupport.Builder authentication;
    private final SamlConfigurationSupport.Builder saml;
    private final Saml2ConfigurationSupport.Builder saml2;
    private final SamlAssertionConfigurationSupport.Builder samlAssertion;
    private final Saml2SsoConfigurationSupport.Builder saml2Sso;

    protected AbstractBuilderSteps(ProfileConfigurationAccess config) {

        common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
        authentication = config != null ? AuthenticationConfigurationSupport.from(config.authenticationConfigurationSupport()) : AuthenticationConfigurationSupport.builder();
        saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
        saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();
        samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport()) : SamlAssertionConfigurationSupport.builder();
        saml2Sso = config != null ? Saml2SsoConfigurationSupport.from(config.saml2SsoConfigurationSupport()) : Saml2SsoConfigurationSupport.builder();
    }

    public CommonConfigurationSupport.Builder commonBuilder() {

        return common;
    }

    public AuthenticationConfigurationSupport.Builder authenticationBuilder() {

        return authentication;
    }

    public SamlConfigurationSupport.Builder samlBuilder() {

        return saml;
    }

    public Saml2ConfigurationSupport.Builder saml2Builder() {

        return saml2;
    }

    public SamlAssertionConfigurationSupport.Builder samlAssertionBuilder() {

        return samlAssertion;
    }

    public Saml2SsoConfigurationSupport.Builder saml2SsoBuilder() {

        return saml2Sso;
    }

    @SuppressWarnings("unchecked")
    protected B self() {

        return (B) this;
    }
}

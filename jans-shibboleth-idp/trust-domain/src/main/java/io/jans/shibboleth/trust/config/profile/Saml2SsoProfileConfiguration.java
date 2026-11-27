package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.AuthenticationConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2SsoConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlAssertionConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.support.AuthenticationConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.kernel.Result;

import java.util.Objects;

public record Saml2SsoProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    AuthenticationConfigurationSupport authenticationConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport,
    SamlAssertionConfigurationSupport samlAssertionConfigurationSupport,
    Saml2SsoConfigurationSupport saml2SsoConfigurationSupport)
    implements CommonConfigurationCapable, AuthenticationConfigurationCapable, SamlConfigurationCapable,
        SamlAssertionConfigurationCapable, Saml2ConfigurationCapable, Saml2SsoConfigurationCapable {

    public Saml2SsoProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(authenticationConfigurationSupport, "authenticationConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(saml2ConfigurationSupport, "saml2ConfigurationSupport");
        Objects.requireNonNull(samlAssertionConfigurationSupport, "samlAssertionConfigurationSupport");
        Objects.requireNonNull(saml2SsoConfigurationSupport, "saml2SsoConfigurationSupport");
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_SSO;
    }

    @Override
    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationConfigurationSupport.authenticationResultReusePolicy();
    }
    //End Authentication profile configuration 
    //End Saml configuration 

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2SsoProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder extends Steps<Builder> {

        public Builder(Saml2SsoProfileConfiguration config) {

            super(config);
        }
    }

    private static class Steps<B extends Steps<B>>
        implements CommonBuilderSteps<B>, AuthenticationBuilderSteps<B>, SamlBuilderSteps<B>,
        Saml2BuilderSteps<B>, SamlAssertionBuilderSteps<B>, Saml2SsoBuilderSteps<B> {

        private final CommonConfigurationSupport.Builder common;
        private final AuthenticationConfigurationSupport.Builder auth;
        private final SamlConfigurationSupport.Builder saml;
        private final Saml2ConfigurationSupport.Builder saml2;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private final Saml2SsoConfigurationSupport.Builder saml2sso;

        private Steps(Saml2SsoProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            auth = config != null ? AuthenticationConfigurationSupport.from(config.authenticationConfigurationSupport()) : AuthenticationConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport()) : SamlAssertionConfigurationSupport.builder();
            saml2sso = config != null ? Saml2SsoConfigurationSupport.from(config.saml2SsoConfigurationSupport()) : Saml2SsoConfigurationSupport.builder();
        }

        @Override
        public CommonConfigurationSupport.Builder commonBuilder() {

            return common;
        }

        @Override
        public AuthenticationConfigurationSupport.Builder authenticationBuilder() {

            return auth;
        }

        @Override
        public SamlConfigurationSupport.Builder samlBuilder() {

            return saml;
        }

        @Override
        public Saml2ConfigurationSupport.Builder saml2Builder() {

            return saml2;
        }

        @Override
        public SamlAssertionConfigurationSupport.Builder samlAssertionBuilder() {

            return samlAssertion;
        }

        @Override
        public Saml2SsoConfigurationSupport.Builder saml2SsoBuilder() {

            return saml2sso;
        }

        @SuppressWarnings("unchecked")
        protected B self() {

            return (B) this;
        }

        @Override
        public B authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

            auth.authenticationResultReusePolicy(policy);
            saml2sso.authenticationResultReusePolicy(policy);

            return self();
        }

        public Result<Saml2SsoProfileConfiguration> build() {

            try {

                return Result.success(new Saml2SsoProfileConfiguration(
                    Builders.require(common.build()),
                    Builders.require(auth.build()),
                    Builders.require(saml.build()),
                    Builders.require(saml2.build()),
                    Builders.require(samlAssertion.build()),
                    Builders.require(saml2sso.build())
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}

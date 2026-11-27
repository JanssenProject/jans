package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2SsoConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlAssertionConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.kernel.Result;

import java.util.Objects;

public record Saml2EcpProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    SamlAssertionConfigurationSupport samlAssertionConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport,
    Saml2SsoConfigurationSupport saml2SsoConfigurationSupport)
    implements CommonConfigurationCapable, SamlConfigurationCapable, SamlAssertionConfigurationCapable,
        Saml2ConfigurationCapable, Saml2SsoConfigurationCapable {

    public Saml2EcpProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(samlAssertionConfigurationSupport, "samlAssertionConfigurationSupport");
        Objects.requireNonNull(saml2ConfigurationSupport, "saml2ConfigurationSupport");
        Objects.requireNonNull(saml2SsoConfigurationSupport, "saml2SsoConfigurationSupport");
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_ECP;
    }
    //End Profile Configuration 
    //End Saml configuration 

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2EcpProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder extends Steps<Builder> {

        public Builder(Saml2EcpProfileConfiguration config) {

            super(config);
        }
    }

    private static class Steps<B extends Steps<B>>
        implements CommonBuilderSteps<B>, SamlBuilderSteps<B>, Saml2BuilderSteps<B>,
        SamlAssertionBuilderSteps<B>, Saml2SsoBuilderSteps<B> {

        private final CommonConfigurationSupport.Builder common;
        private final SamlConfigurationSupport.Builder saml;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private final Saml2ConfigurationSupport.Builder saml2;
        private final Saml2SsoConfigurationSupport.Builder saml2Sso;

        private Steps(Saml2EcpProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            saml  = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport()) : SamlAssertionConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();
            saml2Sso = config != null ? Saml2SsoConfigurationSupport.from(config.saml2SsoConfigurationSupport()) : Saml2SsoConfigurationSupport.builder();
        }

        @Override
        public CommonConfigurationSupport.Builder commonBuilder() {

            return common;
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

            return saml2Sso;
        }

        @SuppressWarnings("unchecked")
        protected B self() {

            return (B) this;
        }

        public Result<Saml2EcpProfileConfiguration> build() {

            try {

                return Result.success(new Saml2EcpProfileConfiguration(
                    Builders.require(common.build()),
                    Builders.require(saml.build()),
                    Builders.require(samlAssertion.build()),
                    Builders.require(saml2.build()),
                    Builders.require(saml2Sso.build())
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}

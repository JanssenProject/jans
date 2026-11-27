package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.kernel.Result;

import java.util.Objects;

public record Saml2LogoutProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport)
    implements CommonConfigurationCapable, SamlConfigurationCapable, Saml2ConfigurationCapable {

    public Saml2LogoutProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(saml2ConfigurationSupport, "saml2ConfigurationSupport");
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_LOGOUT;
    }
    //End Profile Configuration 
    //End Saml configuration 

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2LogoutProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder extends Steps<Builder> {

        public Builder(Saml2LogoutProfileConfiguration config) {

            super(config);
        }
    }

    private static class Steps<B extends Steps<B>>
        implements CommonBuilderSteps<B>, SamlBuilderSteps<B>, Saml2BuilderSteps<B> {

        private final CommonConfigurationSupport.Builder common;
        private final SamlConfigurationSupport.Builder saml;
        private final Saml2ConfigurationSupport.Builder saml2;

        private Steps(Saml2LogoutProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();
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

        @SuppressWarnings("unchecked")
        protected B self() {

            return (B) this;
        }

        public Result<Saml2LogoutProfileConfiguration> build() {

            try {

                return Result.success(new Saml2LogoutProfileConfiguration(
                    Builders.require(common.build()),
                    Builders.require(saml.build()),
                    Builders.require(saml2.build())
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}

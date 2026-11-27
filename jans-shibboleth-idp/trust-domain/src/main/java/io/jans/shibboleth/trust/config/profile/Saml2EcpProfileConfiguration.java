package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
import io.jans.kernel.Result;

import java.util.Objects;

public record Saml2EcpProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    SamlAssertionConfigurationSupport samlAssertionConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport,
    Saml2SsoConfigurationSupport saml2SsoConfigurationSupport)
    implements CommonConfigurationCapable, SamlConfigurationCapable, SamlAssertionConfigurationCapable,
        Saml2ConfigurationCapable, Saml2SsoConfigurationCapable, ProfileConfigurationAccess {

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
        extends AbstractBuilderSteps<B>
        implements CommonBuilderSteps<B>, SamlBuilderSteps<B>, Saml2BuilderSteps<B>,
        SamlAssertionBuilderSteps<B>, Saml2SsoBuilderSteps<B> {

        private Steps(Saml2EcpProfileConfiguration config) {

            super(config);
        }

        public Result<Saml2EcpProfileConfiguration> build() {

            try {

                return Result.success(new Saml2EcpProfileConfiguration(
                    Builders.require(commonBuilder().build()),
                    Builders.require(samlBuilder().build()),
                    Builders.require(samlAssertionBuilder().build()),
                    Builders.require(saml2Builder().build()),
                    Builders.require(saml2SsoBuilder().build())
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}

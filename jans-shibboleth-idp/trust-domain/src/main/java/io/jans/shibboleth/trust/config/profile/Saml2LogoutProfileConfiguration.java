package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
import io.jans.kernel.Result;

import java.util.Objects;

public record Saml2LogoutProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport)
    implements CommonConfigurationCapable, SamlConfigurationCapable, Saml2ConfigurationCapable,
        ProfileConfigurationAccess {

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
        extends AbstractBuilderSteps<B>
        implements CommonBuilderSteps<B>, SamlBuilderSteps<B>, Saml2BuilderSteps<B> {

        private Steps(Saml2LogoutProfileConfiguration config) {

            super(config);
        }

        public Result<Saml2LogoutProfileConfiguration> build() {

            try {

                return Result.success(new Saml2LogoutProfileConfiguration(
                    Builders.require(commonBuilder().build()),
                    Builders.require(samlBuilder().build()),
                    Builders.require(saml2Builder().build())
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}

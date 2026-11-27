package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
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
        SamlAssertionConfigurationCapable, Saml2ConfigurationCapable, Saml2SsoConfigurationCapable,
        ProfileConfigurationAccess {

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
        extends AbstractBuilderSteps<B>
        implements CommonBuilderSteps<B>, AuthenticationBuilderSteps<B>, SamlBuilderSteps<B>,
        Saml2BuilderSteps<B>, SamlAssertionBuilderSteps<B>, Saml2SsoBuilderSteps<B> {

        private Steps(Saml2SsoProfileConfiguration config) {

            super(config);
        }

        @Override
        public B authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

            authenticationBuilder().authenticationResultReusePolicy(policy);
            saml2SsoBuilder().authenticationResultReusePolicy(policy);

            return self();
        }

        public Result<Saml2SsoProfileConfiguration> build() {

            try {

                return Result.success(new Saml2SsoProfileConfiguration(
                    Builders.require(commonBuilder().build()),
                    Builders.require(authenticationBuilder().build()),
                    Builders.require(samlBuilder().build()),
                    Builders.require(saml2Builder().build()),
                    Builders.require(samlAssertionBuilder().build()),
                    Builders.require(saml2SsoBuilder().build())
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}

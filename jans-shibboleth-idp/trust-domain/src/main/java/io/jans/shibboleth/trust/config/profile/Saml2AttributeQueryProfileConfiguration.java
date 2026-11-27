package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
import io.jans.kernel.Result;

import java.util.Objects;

public record Saml2AttributeQueryProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    SamlAssertionConfigurationSupport samlAssertionConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport,
    AssertionEncryptionPolicy assertionEncryptionPolicy,
    AttributeEncryptionPolicy attributeEncryptionPolicy,
    FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy)
    implements CommonConfigurationCapable, SamlConfigurationCapable, SamlAssertionConfigurationCapable,
        Saml2ConfigurationCapable, AssertionEncryptionPolicyCapable, AttributeEncryptionPolicyCapable,
        ProfileConfigurationAccess {

    public Saml2AttributeQueryProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(samlAssertionConfigurationSupport, "samlAssertionConfigurationSupport");
        Objects.requireNonNull(saml2ConfigurationSupport, "saml2ConfigurationSupport");
        Objects.requireNonNull(assertionEncryptionPolicy, "assertionEncryptionPolicy");
        Objects.requireNonNull(attributeEncryptionPolicy, "attributeEncryptionPolicy");
        Objects.requireNonNull(friendlyNameRandomizationPolicy, "friendlyNameRandomizationPolicy");
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_ATTRIBUTE_QUERY;
    }
    //End Saml Assertion configuration

    public FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy() {

        return friendlyNameRandomizationPolicy;
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2AttributeQueryProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder extends Steps<Builder> {

        public Builder(Saml2AttributeQueryProfileConfiguration config) {

            super(config);
        }
    }

    private static class Steps<B extends Steps<B>>
        extends AbstractBuilderSteps<B>
        implements CommonBuilderSteps<B>, SamlBuilderSteps<B>, Saml2BuilderSteps<B>, SamlAssertionBuilderSteps<B> {

        private AssertionEncryptionPolicy assertionEncryptionPolicy;
        private AttributeEncryptionPolicy attributeEncryptionPolicy;
        private FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;

        private Steps(Saml2AttributeQueryProfileConfiguration config) {

            super(config);

            assertionEncryptionPolicy = config != null ? config.assertionEncryptionPolicy : null;
            attributeEncryptionPolicy = config != null ? config.attributeEncryptionPolicy : null;
            friendlyNameRandomizationPolicy = config != null ? config.friendlyNameRandomizationPolicy : null;
        }

        public B assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

            assertionEncryptionPolicy = policy;
            return self();
        }

        public B attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

            attributeEncryptionPolicy = policy;
            return self();
        }

        public B friendlyRandomizationPolicy(FriendlyNameRandomizationPolicy policy) {

            friendlyNameRandomizationPolicy = policy;
            return self();
        }

        public Result<Saml2AttributeQueryProfileConfiguration> build() {

            try {

                return Result.success(new Saml2AttributeQueryProfileConfiguration(
                    Builders.require(commonBuilder().build()),
                    Builders.require(samlBuilder().build()),
                    Builders.require(samlAssertionBuilder().build()),
                    Builders.require(saml2Builder().build()),
                    Builders.require(assertionEncryptionPolicy, Saml2AttributeQueryProfileConfiguration.class, "assertionEncryptionPolicy"),
                    Builders.require(attributeEncryptionPolicy, Saml2AttributeQueryProfileConfiguration.class, "attributeEncryptionPolicy"),
                    Builders.require(friendlyNameRandomizationPolicy, Saml2AttributeQueryProfileConfiguration.class, "friendlyNameRandomizationPolicy")
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}

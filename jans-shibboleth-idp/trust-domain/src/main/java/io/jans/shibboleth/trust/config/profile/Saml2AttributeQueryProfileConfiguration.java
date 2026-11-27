package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.AssertionEncryptionPolicyCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.AttributeEncryptionPolicyCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlAssertionConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.kernel.RequiredValueMissing;
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
        Saml2ConfigurationCapable, AssertionEncryptionPolicyCapable, AttributeEncryptionPolicyCapable {

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
        implements CommonBuilderSteps<B>, SamlBuilderSteps<B>, Saml2BuilderSteps<B>, SamlAssertionBuilderSteps<B> {

        private final CommonConfigurationSupport.Builder common;
        private final SamlConfigurationSupport.Builder saml;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private final Saml2ConfigurationSupport.Builder saml2;
        private AssertionEncryptionPolicy assertionEncryptionPolicy;
        private AttributeEncryptionPolicy attributeEncryptionPolicy;
        private FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;

        private Steps(Saml2AttributeQueryProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport()) : SamlAssertionConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();

            assertionEncryptionPolicy = config != null ? config.assertionEncryptionPolicy : null;
            attributeEncryptionPolicy = config != null ? config.attributeEncryptionPolicy : null;
            friendlyNameRandomizationPolicy = config != null ? config.friendlyNameRandomizationPolicy : null;
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

        @SuppressWarnings("unchecked")
        protected B self() {

            return (B) this;
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

            CommonConfigurationSupport commonValue;
            SamlConfigurationSupport samlValue;
            SamlAssertionConfigurationSupport samlAssertionValue;
            Saml2ConfigurationSupport saml2Value;

            try {

                commonValue = Builders.require(common.build());
                samlValue = Builders.require(saml.build());
                samlAssertionValue = Builders.require(samlAssertion.build());
                saml2Value = Builders.require(saml2.build());
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }

            if (assertionEncryptionPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        Saml2AttributeQueryProfileConfiguration.class, "assertionEncryptionPolicy"));
            }

            if (attributeEncryptionPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        Saml2AttributeQueryProfileConfiguration.class, "attributeEncryptionPolicy"));
            }

            if (friendlyNameRandomizationPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        Saml2AttributeQueryProfileConfiguration.class, "friendNameRandomizationPolicy"));
            }

            return Result.success(new Saml2AttributeQueryProfileConfiguration(
                commonValue, samlValue, samlAssertionValue,
                saml2Value, assertionEncryptionPolicy, attributeEncryptionPolicy, friendlyNameRandomizationPolicy
            ));
        }
    }
}

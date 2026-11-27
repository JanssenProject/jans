package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.AssertionEncryptionPolicyCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.AttributeEncryptionPolicyCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import java.util.Objects;

public record Saml2ArtifactResolutionProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport,
    AssertionSigningPolicy assertionSigningPolicy,
    AssertionEncryptionPolicy assertionEncryptionPolicy,
    AttributeEncryptionPolicy attributeEncryptionPolicy)
    implements CommonConfigurationCapable, SamlConfigurationCapable, Saml2ConfigurationCapable,
        AssertionEncryptionPolicyCapable, AttributeEncryptionPolicyCapable {

    public Saml2ArtifactResolutionProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(saml2ConfigurationSupport, "saml2ConfigurationSupport");
        Objects.requireNonNull(assertionSigningPolicy, "assertionSigningPolicy");
        Objects.requireNonNull(assertionEncryptionPolicy, "assertionEncryptionPolicy");
        Objects.requireNonNull(attributeEncryptionPolicy, "attributeEncryptionPolicy");
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_ARTIFACT_RESOLUTION;
    }
    //End Profile Configuration 
    //End Saml configuration 

    public AssertionSigningPolicy getAssertionSigningPolicy() {

        return assertionSigningPolicy;
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2ArtifactResolutionProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder extends Steps<Builder> {

        public Builder(Saml2ArtifactResolutionProfileConfiguration config) {

            super(config);
        }
    }

    private static class Steps<B extends Steps<B>>
        implements CommonBuilderSteps<B>, SamlBuilderSteps<B>, Saml2BuilderSteps<B> {

        private final CommonConfigurationSupport.Builder common;
        private final SamlConfigurationSupport.Builder saml;
        private final Saml2ConfigurationSupport.Builder saml2;
        private AssertionSigningPolicy assertionSigningPolicy;
        private AssertionEncryptionPolicy assertionEncryptionPolicy;
        private AttributeEncryptionPolicy attributeEncryptionPolicy;

        private Steps(Saml2ArtifactResolutionProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();

            assertionSigningPolicy = config != null ? config.assertionSigningPolicy : null;
            assertionEncryptionPolicy = config != null ? config.assertionEncryptionPolicy : null;
            attributeEncryptionPolicy = config != null ? config.attributeEncryptionPolicy : null;
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

        public B assertionSigningPolicy(AssertionSigningPolicy policy) {

            assertionSigningPolicy = policy;
            return self();
        }

        public B assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

            assertionEncryptionPolicy = policy;
            return self();
        }

        public B attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

            attributeEncryptionPolicy = policy;
            return self();
        }

        public Result<Saml2ArtifactResolutionProfileConfiguration> build() {

            CommonConfigurationSupport commonValue;
            SamlConfigurationSupport samlValue;
            Saml2ConfigurationSupport saml2Value;

            try {

                commonValue = Builders.require(common.build());
                samlValue = Builders.require(saml.build());
                saml2Value = Builders.require(saml2.build());
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }

            if (assertionSigningPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        Saml2ArtifactResolutionProfileConfiguration.class, "assertionSigningPolicy"));
            }

            if (assertionEncryptionPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        Saml2ArtifactResolutionProfileConfiguration.class, "assertionEncryptionPolicy"));
            }

            if (attributeEncryptionPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        Saml2ArtifactResolutionProfileConfiguration.class, "attributeEncryptionPolicy"));
            }

            return Result.success(new Saml2ArtifactResolutionProfileConfiguration(
                commonValue, samlValue, saml2Value,
                assertionSigningPolicy, assertionEncryptionPolicy, attributeEncryptionPolicy)
            );
        }
    }
}

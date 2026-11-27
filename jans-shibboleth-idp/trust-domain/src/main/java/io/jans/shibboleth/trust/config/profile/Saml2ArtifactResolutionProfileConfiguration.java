package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
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
        AssertionEncryptionPolicyCapable, AttributeEncryptionPolicyCapable, ProfileConfigurationAccess {

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
        extends AbstractBuilderSteps<B>
        implements CommonBuilderSteps<B>, SamlBuilderSteps<B>, Saml2BuilderSteps<B> {

        private AssertionSigningPolicy assertionSigningPolicy;
        private AssertionEncryptionPolicy assertionEncryptionPolicy;
        private AttributeEncryptionPolicy attributeEncryptionPolicy;

        private Steps(Saml2ArtifactResolutionProfileConfiguration config) {

            super(config);

            assertionSigningPolicy = config != null ? config.assertionSigningPolicy : null;
            assertionEncryptionPolicy = config != null ? config.assertionEncryptionPolicy : null;
            attributeEncryptionPolicy = config != null ? config.attributeEncryptionPolicy : null;
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

            try {

                return Result.success(new Saml2ArtifactResolutionProfileConfiguration(
                    Builders.require(commonBuilder().build()),
                    Builders.require(samlBuilder().build()),
                    Builders.require(saml2Builder().build()),
                    Builders.require(assertionSigningPolicy, Saml2ArtifactResolutionProfileConfiguration.class, "assertionSigningPolicy"),
                    Builders.require(assertionEncryptionPolicy, Saml2ArtifactResolutionProfileConfiguration.class, "assertionEncryptionPolicy"),
                    Builders.require(attributeEncryptionPolicy, Saml2ArtifactResolutionProfileConfiguration.class, "attributeEncryptionPolicy")
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}

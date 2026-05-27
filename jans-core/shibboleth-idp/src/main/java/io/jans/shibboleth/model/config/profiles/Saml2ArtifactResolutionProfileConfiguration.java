package io.jans.shibboleth.model.config.profiles;

import io.jans.common.Result;
import io.jans.shibboleth.model.config.profiles.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.SamlAssertionConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionTimeCondition;
import io.jans.shibboleth.model.config.profiles.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.model.config.profiles.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.model.config.profiles.common.InterceptorFlows;
import io.jans.shibboleth.model.config.profiles.common.MessageSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.config.profiles.common.ProfileStatus;
import io.jans.shibboleth.model.config.profiles.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.model.config.profiles.support.CommonConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.SamlConfigurationSupport;
import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.TrustResult;

import java.time.Duration;
import java.util.Objects;

public class Saml2ArtifactResolutionProfileConfiguration implements CommonConfigurationCapable, SamlConfigurationCapable, Saml2ConfigurationCapable {
    
    

    private final CommonConfigurationSupport commonConfigurationSupport;
    private final SamlConfigurationSupport samlConfigurationSupport;
    private final Saml2ConfigurationSupport saml2ConfigurationSupport;

    private final AssertionSigningPolicy    assertionSigningPolicy;
    private final AssertionEncryptionPolicy assertionEncryptionPolicy;
    private final AttributeEncryptionPolicy attributeEncryptionPolicy;


    private Saml2ArtifactResolutionProfileConfiguration(
        CommonConfigurationSupport commonConfigurationSupport, SamlConfigurationSupport samlConfigurationSupport,
        Saml2ConfigurationSupport saml2ConfigurationSupport, AssertionSigningPolicy assertionSigningPolicy,
        AssertionEncryptionPolicy assertionEncryptionPolicy,AttributeEncryptionPolicy attributeEncryptionPolicy ) {
        
        this.commonConfigurationSupport = commonConfigurationSupport;
        this.samlConfigurationSupport = samlConfigurationSupport;
        this.saml2ConfigurationSupport = saml2ConfigurationSupport;
        this.assertionSigningPolicy = assertionSigningPolicy;
        this.assertionEncryptionPolicy = assertionEncryptionPolicy;
        this.attributeEncryptionPolicy = attributeEncryptionPolicy;
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_ARTIFACT_RESOLUTION;
    }

    @Override
    public ProfileStatus getStatus() {

        return commonConfigurationSupport.getStatus();
    }

    @Override
    public InterceptorFlows getInboundFlows() {

        return commonConfigurationSupport.getInboundFlows();
    }

    @Override
    public InterceptorFlows getOutboundFlows() {

        return commonConfigurationSupport.getOutboundFlows();
    }
    //End Profile Configuration 

    //Saml configuration 
    @Override
    public MessageSigningPolicy getMessageSigningPolicy() {

        return samlConfigurationSupport.getMessageSigningPolicy();
    }
    //End Saml configuration 

    //Saml2 Configuration
    @Override
    public RequestSignatureValidationPolicy getRequestSignatureValidationPolicy() {

        return saml2ConfigurationSupport.getRequestSignatureValidationPolicy();
    }

    @Override 
    public EncryptionFallbackPolicy getEncryptionFallbackPolicy() {

        return saml2ConfigurationSupport.getEncryptionFallbackPolicy();
    }

    @Override 
    public NameIdEncryptionPolicy getNameIdEncryptionPolicy() {

        return saml2ConfigurationSupport.getNameIdEncryptionPolicy();
    }
    //End Saml2 Configuration 

    public AssertionEncryptionPolicy getAssertionEncryptionPolicy() {

        return assertionEncryptionPolicy;
    }

    public AttributeEncryptionPolicy getAttributeEncryptionPolicy() {

        return attributeEncryptionPolicy;
    }

    public AssertionSigningPolicy getAssertionSigningPolicy() {

        return assertionSigningPolicy;
    }

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false;

        Saml2ArtifactResolutionProfileConfiguration other = (Saml2ArtifactResolutionProfileConfiguration) o;

        return Objects.equals(commonConfigurationSupport,other.commonConfigurationSupport)
            && Objects.equals(samlConfigurationSupport,samlConfigurationSupport)
            && Objects.equals(saml2ConfigurationSupport,other.saml2ConfigurationSupport)
            && assertionSigningPolicy == other.assertionSigningPolicy
            && assertionEncryptionPolicy == other.assertionEncryptionPolicy 
            && attributeEncryptionPolicy == other.attributeEncryptionPolicy;
    }

    @Override
    public int hashCode() {

        return Objects.hash(commonConfigurationSupport,samlConfigurationSupport,saml2ConfigurationSupport
            ,assertionSigningPolicy,assertionEncryptionPolicy,attributeEncryptionPolicy);
    }


    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2ArtifactResolutionProfileConfiguration config) {
        
        return new Builder(config);
    }

    public static class Builder {

        private final CommonConfigurationSupport.Builder common;
        private final SamlConfigurationSupport.Builder saml;
        private final Saml2ConfigurationSupport.Builder saml2;

        private AssertionSigningPolicy assertionSigningPolicy;
        private AssertionEncryptionPolicy assertionEncryptionPolicy;
        private AttributeEncryptionPolicy attributeEncryptionPolicy;

        public Builder(Saml2ArtifactResolutionProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport) : CommonConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport) : SamlConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport) : Saml2ConfigurationSupport.builder();

            assertionSigningPolicy = config != null ? config.assertionSigningPolicy : null;
            assertionEncryptionPolicy = config != null ? config.assertionEncryptionPolicy : null;
            attributeEncryptionPolicy = config != null ? config.attributeEncryptionPolicy : null;
        }

        public Builder status(ProfileStatus status) {

            common.status(status);
            return this;
        }

        public Builder inboundFlows(InterceptorFlows flows) {

            common.inboundFlows(flows);
            return this;
        }

        public Builder outboundFlows(InterceptorFlows flows) {

            common.outboundFlows(flows);
            return this;
        }

        public Builder messageSigningPolicy(MessageSigningPolicy policy) {

            saml.messageSigningPolicy(policy);
            return this;
        }

        public Builder requestSignatureValidationPolicy(RequestSignatureValidationPolicy policy) {

            saml2.requestSignatureValidationPolicy(policy);
            return this;
        }

        public Builder encryptionFallbackPolicy(EncryptionFallbackPolicy policy) {

            saml2.encryptionFallbackPolicy(policy);
            return this;
        }

        public Builder nameIdEncryptionPolicy(NameIdEncryptionPolicy policy) {

            saml2.nameIdEncryptionPolicy(policy);
            return this;
        }

        public Builder assertionSigningPolicy(AssertionSigningPolicy policy) {

            assertionSigningPolicy = policy;
            return this;
        }

        public Builder assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

            assertionEncryptionPolicy  = policy;
            return this;
        }

        public Builder attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

            attributeEncryptionPolicy = policy;
            return this;
        }


        public TrustResult<Saml2ArtifactResolutionProfileConfiguration> build() {

            TrustResult<CommonConfigurationSupport> commonResult = common.build();
            if (commonResult.isFailure()) {

                return TrustResult.failure(commonResult.getError());
            }

            TrustResult<SamlConfigurationSupport> samlResult = saml.build();

            if (samlResult.isFailure()) {

                return TrustResult.failure(samlResult.getError());
            }

            TrustResult<Saml2ConfigurationSupport> saml2Result = saml2.build();
            if (saml2Result.isFailure()) {

                return TrustResult.failure(saml2Result.getError());
            }

            if (assertionSigningPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("assertionSigningPolicy"));
            }

            if (assertionEncryptionPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("assertionEncryptionPolicy"));
            }

            if (attributeEncryptionPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("attributeEncryptionPolicy"));
            }

            return TrustResult.success(new Saml2ArtifactResolutionProfileConfiguration(
                commonResult.getValue(),samlResult.getValue(),saml2Result.getValue(),
                assertionSigningPolicy, assertionEncryptionPolicy, attributeEncryptionPolicy)
            );
        }
    }
}
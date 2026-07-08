package io.jans.shibboleth.model.config.profiles;

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

public class Saml2AttributeQueryProfileConfiguration implements CommonConfigurationCapable, SamlConfigurationCapable, 
    SamlAssertionConfigurationCapable,Saml2ConfigurationCapable {
    
    private final CommonConfigurationSupport commonConfigurationSupport;
    private final SamlConfigurationSupport samlConfigurationSupport;
    private final SamlAssertionConfigurationSupport samlAssertionConfigurationSupport;
    private final Saml2ConfigurationSupport saml2ConfigurationSupport;

    private final AssertionEncryptionPolicy assertionEncryptionPolicy;
    private final AttributeEncryptionPolicy attributeEncryptionPolicy;
    private final FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;


    private Saml2AttributeQueryProfileConfiguration(
        CommonConfigurationSupport commonConfigurationSupport, SamlConfigurationSupport samlConfigurationSupport,
        SamlAssertionConfigurationSupport samlAssertionConfigurationSupport, Saml2ConfigurationSupport saml2ConfigurationSupport,
        AssertionEncryptionPolicy assertionEncryptionPolicy, AttributeEncryptionPolicy attributeEncryptionPolicy,
        FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy ) {

        this.commonConfigurationSupport = commonConfigurationSupport;
        this.samlConfigurationSupport = samlConfigurationSupport;
        this.samlAssertionConfigurationSupport = samlAssertionConfigurationSupport;
        this.saml2ConfigurationSupport = saml2ConfigurationSupport;
        this.assertionEncryptionPolicy = assertionEncryptionPolicy;
        this.attributeEncryptionPolicy = attributeEncryptionPolicy;
        this.friendlyNameRandomizationPolicy = friendlyNameRandomizationPolicy;
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_ATTRIBUTE_QUERY;
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

    //Saml Assertion configuration
    @Override
    public AssertionTimeCondition getAssertionTimeCondition() {

        return samlAssertionConfigurationSupport.getAssertionTimeCondition();
    }

    @Override
    public Duration getAssertionLifetime() {

        return samlAssertionConfigurationSupport.getAssertionLifetime();
    }

    @Override
    public AssertionSigningPolicy getAssertionSigningPolicy() {

        return samlAssertionConfigurationSupport.getAssertionSigningPolicy();
    }
    //End Saml Assertion configuration

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

    public FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy() {

        return friendlyNameRandomizationPolicy;
    }

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true; 

        if ( o == null || getClass() != o.getClass() ) return false;

        Saml2AttributeQueryProfileConfiguration other = (Saml2AttributeQueryProfileConfiguration) o;

        return Objects.equals(commonConfigurationSupport,other.commonConfigurationSupport)
            && Objects.equals(samlConfigurationSupport,other.samlConfigurationSupport)
            && Objects.equals(samlAssertionConfigurationSupport,other.samlAssertionConfigurationSupport)
            && Objects.equals(saml2ConfigurationSupport,other.saml2ConfigurationSupport)
            && assertionEncryptionPolicy == other.assertionEncryptionPolicy
            && attributeEncryptionPolicy == other.attributeEncryptionPolicy
            && friendlyNameRandomizationPolicy == other.friendlyNameRandomizationPolicy;
    }

    @Override
    public int hashCode() {

        return Objects.hash(
            commonConfigurationSupport,samlConfigurationSupport,
            samlAssertionConfigurationSupport,saml2ConfigurationSupport,
            assertionEncryptionPolicy,attributeEncryptionPolicy,friendlyNameRandomizationPolicy
        );
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2AttributeQueryProfileConfiguration config) {

        return new Builder(config);
    }
    
    public static class Builder {

        private final CommonConfigurationSupport.Builder common;
        private final SamlConfigurationSupport.Builder saml;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private final Saml2ConfigurationSupport.Builder saml2;
        private AssertionEncryptionPolicy assertionEncryptionPolicy;
        private AttributeEncryptionPolicy attributeEncryptionPolicy;
        private FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;

        public Builder(Saml2AttributeQueryProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport) : CommonConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport) : SamlConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport) : SamlAssertionConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport) : Saml2ConfigurationSupport.builder();

            assertionEncryptionPolicy = config != null ? config.assertionEncryptionPolicy : null;
            attributeEncryptionPolicy = config != null ? config.attributeEncryptionPolicy : null;
            friendlyNameRandomizationPolicy = config != null ? config.friendlyNameRandomizationPolicy : null;
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

        public Builder assertionSigningPolicy(AssertionSigningPolicy policy) {

            samlAssertion.assertionSigningPolicy(policy);
            return this;
        }

        public Builder assertionTimeCondition(AssertionTimeCondition condition) {

            samlAssertion.assertionTimeCondition(condition);
            return this;
        }

        public Builder assertionLifetime(Duration lifetime) {

            samlAssertion.assertionLifetime(lifetime);
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

        public Builder assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

            assertionEncryptionPolicy = policy;
            return this;
        }

        public Builder attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

            attributeEncryptionPolicy = policy;
            return this;
        }

        public Builder friendlyRandomizationPolicy(FriendlyNameRandomizationPolicy policy) {

            friendlyNameRandomizationPolicy = policy;
            return this;
        }

        public TrustResult<Saml2AttributeQueryProfileConfiguration> build() {
            
            TrustResult<CommonConfigurationSupport> commonResult = common.build();

            if (commonResult.isFailure()) {

                return TrustResult.failure(commonResult.getError());
            }

            TrustResult<SamlConfigurationSupport> samlResult = saml.build();

            if (samlResult.isFailure()) {

                return TrustResult.failure(samlResult.getError());
            }

            TrustResult<SamlAssertionConfigurationSupport> samlAssertionResult = samlAssertion.build();

            if (samlAssertionResult.isFailure()) {

                return TrustResult.failure(samlAssertionResult.getError());
            }

            TrustResult<Saml2ConfigurationSupport> saml2Result = saml2.build();

            if (saml2Result.isFailure()) {

                return TrustResult.failure(saml2Result.getError());
            }

            if (assertionEncryptionPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("assertionEncryptionPolicy"));
            }

            if (attributeEncryptionPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("attributeEncryptionPolicy"));
            }

            if (friendlyNameRandomizationPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("friendNameRandomizationPolicy"));
            }

            return TrustResult.success(new Saml2AttributeQueryProfileConfiguration(
                commonResult.getValue(),samlResult.getValue(),samlAssertionResult.getValue(),
                saml2Result.getValue(),assertionEncryptionPolicy,attributeEncryptionPolicy,friendlyNameRandomizationPolicy
            ));

        }
    }
}
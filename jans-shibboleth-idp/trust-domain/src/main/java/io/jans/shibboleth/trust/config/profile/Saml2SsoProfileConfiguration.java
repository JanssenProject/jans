package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.AuthenticationConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2SsoConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlAssertionConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.InterceptorFlows;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdentifiers;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;
import io.jans.shibboleth.trust.config.profile.support.AuthenticationConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Duration;
import java.util.Objects;

public class Saml2SsoProfileConfiguration implements CommonConfigurationCapable, AuthenticationConfigurationCapable, SamlConfigurationCapable, 
    SamlAssertionConfigurationCapable, Saml2ConfigurationCapable,Saml2SsoConfigurationCapable {
    

    private final CommonConfigurationSupport commonConfigurationSupport;
    private final AuthenticationConfigurationSupport authenticationConfigurationSupport;
    private final SamlConfigurationSupport samlConfigurationSupport;
    private final Saml2ConfigurationSupport saml2ConfigurationSupport;
    private final SamlAssertionConfigurationSupport samlAssertionConfigurationSupport;
    private final Saml2SsoConfigurationSupport saml2SsoConfigurationSupport;

    private Saml2SsoProfileConfiguration(
        CommonConfigurationSupport commonConfigurationSupport, AuthenticationConfigurationSupport authenticationConfigurationSupport,
        SamlConfigurationSupport samlConfigurationSupport, Saml2ConfigurationSupport saml2ConfigurationSupport,
        SamlAssertionConfigurationSupport samlAssertionConfigurationSupport, Saml2SsoConfigurationSupport saml2SsoConfigurationSupport) {

        this.commonConfigurationSupport = commonConfigurationSupport;
        this.authenticationConfigurationSupport = authenticationConfigurationSupport;
        this.samlConfigurationSupport = samlConfigurationSupport;
        this.saml2ConfigurationSupport = saml2ConfigurationSupport;
        this.samlAssertionConfigurationSupport = samlAssertionConfigurationSupport;
        this.saml2SsoConfigurationSupport = saml2SsoConfigurationSupport;
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_SSO;
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

    //Authentication profile configuration
    @Override
    public InterceptorFlows getPostAuthenticationFlows() {

        return authenticationConfigurationSupport.getPostAuthenticationFlows();
    }

    @Override
    public Duration getMaxAuthenticationAge() {

        return authenticationConfigurationSupport.getMaximumAuthenticationAge();
    }

    @Override
    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationConfigurationSupport.getAuthenticationResultReusePolicy();
    }
    //End Authentication profile configuration 

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
    //End Saml assertion configuration 

    //Saml2SSo configuration
    @Override
    public AssertionEncryptionPolicy getAssertionEncryptionPolicy() {

        return saml2SsoConfigurationSupport.getAssertionEncryptionPolicy();
    }

    @Override
    public AttributeEncryptionPolicy getAttributeEncryptionPolicy() {

        return saml2SsoConfigurationSupport.getAttributeEncryptionPolicy();
    }

    @Override
    public Duration getMaximumSPSessionLifetime() {

        return saml2SsoConfigurationSupport.getMaximumSPSessionLifetime();
    }

    @Override
    public EndpointValidationPolicy getEndpointValidationPolicy() {

        return saml2SsoConfigurationSupport.getEndpointValidationPolicy();
    }

    @Override
    public AttributeStatementPolicy getAttributeStatementPolicy() {

        return saml2SsoConfigurationSupport.getAttributeStatementPolicy();
    }

    @Override
    public FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy() {

        return saml2SsoConfigurationSupport.getFriendlyNameRandomizationPolicy();
    }

    @Override
    public NameIdentifiers getNameIdFormatPrecedence() {

        return saml2SsoConfigurationSupport.getNameIdFormatPrecedence();
    }

    @Override 
    public RequestSigningRequirement getRequestSigningRequirement() {

        return saml2SsoConfigurationSupport.getRequestSigningRequirement();
    }
    //End Saml2Sso configuration

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false;

        Saml2SsoProfileConfiguration other = (Saml2SsoProfileConfiguration) o; 

        return Objects.equals(commonConfigurationSupport,other.commonConfigurationSupport)
            && Objects.equals(authenticationConfigurationSupport,other.authenticationConfigurationSupport)
            && Objects.equals(samlConfigurationSupport,other.samlConfigurationSupport) 
            && Objects.equals(saml2ConfigurationSupport,other.saml2ConfigurationSupport)
            && Objects.equals(samlAssertionConfigurationSupport,other.samlAssertionConfigurationSupport)
            && Objects.equals(saml2SsoConfigurationSupport,other.saml2SsoConfigurationSupport);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
            commonConfigurationSupport,authenticationConfigurationSupport,samlConfigurationSupport,
            saml2ConfigurationSupport,samlAssertionConfigurationSupport,saml2SsoConfigurationSupport
        );
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2SsoProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder {

        private final CommonConfigurationSupport.Builder common;
        private final AuthenticationConfigurationSupport.Builder auth;
        private final SamlConfigurationSupport.Builder saml;
        private final Saml2ConfigurationSupport.Builder saml2;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private final Saml2SsoConfigurationSupport.Builder saml2sso;

        public Builder(Saml2SsoProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport) : CommonConfigurationSupport.builder();
            auth = config != null ? AuthenticationConfigurationSupport.from(config.authenticationConfigurationSupport) : AuthenticationConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport) : SamlConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport) : Saml2ConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport) : SamlAssertionConfigurationSupport.builder();
            saml2sso = config != null ? Saml2SsoConfigurationSupport.from(config.saml2SsoConfigurationSupport) : Saml2SsoConfigurationSupport.builder();
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

        public Builder postAuthenticationFlows(InterceptorFlows flows) {

            auth.postAuthenticationFlows(flows);
            return this;
        }

        public Builder authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

            auth.authenticationResultReusePolicy(policy);
            saml2sso.authenticationResultReusePolicy(policy);

            return this;
        }

        public Builder maximumAuthenticationAge(Duration age) {

            auth.maximumAuthenticationAge(age);
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
        
        public Builder assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

            saml2sso.assertionEncryptionPolicy(policy);
            return this;
        }

        public Builder attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

            saml2sso.attributeEncryptionPolicy(policy);
            return this;
        }

        public Builder maximumSPSessionLifetime(Duration lifetime) {

            saml2sso.maximumSPSessionLifetime(lifetime);
            return this;
        }

        public Builder endpointValidationPolicy(EndpointValidationPolicy policy) {

            saml2sso.endpointValidationPolicy(policy);
            return  this;
        }

        public Builder attributeStatementPolicy(AttributeStatementPolicy policy) {

            saml2sso.attributeStatementPolicy(policy);
            return this;
        }

        public Builder friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy policy) {

            saml2sso.friendlyNameRandomizationPolicy(policy);
            return this;
        }

        public Builder nameIdFormatPrecedence(NameIdentifiers nameIdentifiers) {

            saml2sso.nameIdFormatPrecedence(nameIdentifiers);
            return this;
        }

        public Builder requestSigningRequirement(RequestSigningRequirement requirement) {
            
            saml2sso.requestSigningRequirement(requirement);
            return this;
        }

        public Result<Saml2SsoProfileConfiguration> build() {
        
            Result<CommonConfigurationSupport> commonResult = common.build();
            if (commonResult.isFailure()) {

                return Result.failure(commonResult.getError());
            }

            Result<AuthenticationConfigurationSupport> authResult = auth.build();
            if (authResult.isFailure()) {

                return Result.failure(authResult.getError());
            }

            Result<SamlConfigurationSupport> samlResult = saml.build();
            if (samlResult.isFailure()) {

                return Result.failure(samlResult.getError());
            }

            Result<Saml2ConfigurationSupport> saml2Result = saml2.build();
            if(saml2Result.isFailure()) {

                return Result.failure(saml2Result.getError());
            }

            Result<SamlAssertionConfigurationSupport> samlAssertionResult = samlAssertion.build();
            if (samlAssertionResult.isFailure()) {

                return Result.failure(samlAssertionResult.getError());
            }

            Result<Saml2SsoConfigurationSupport> saml2ssoResult = saml2sso.build();
            if (saml2ssoResult.isFailure()) {

                return Result.failure(saml2ssoResult.getError());
            }

            return Result.success(new Saml2SsoProfileConfiguration(
                commonResult.getValue(),authResult.getValue(), samlResult.getValue(),
                saml2Result.getValue(), samlAssertionResult.getValue(), saml2ssoResult.getValue()
            ));
        }

    }
}
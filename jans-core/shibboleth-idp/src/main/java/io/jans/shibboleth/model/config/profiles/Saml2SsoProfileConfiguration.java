package io.jans.shibboleth.model.config.profiles;

import io.jans.shibboleth.model.config.profiles.capabilities.AuthenticationConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.Saml2SsoConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.SamlAssertionConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionTimeCondition;
import io.jans.shibboleth.model.config.profiles.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AttributeStatementPolicy;
import io.jans.shibboleth.model.config.profiles.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.model.config.profiles.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.model.config.profiles.common.EndpointValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.model.config.profiles.common.InterceptorFlows;
import io.jans.shibboleth.model.config.profiles.common.MessageSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdentifiers;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.config.profiles.common.ProfileStatus;
import io.jans.shibboleth.model.config.profiles.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.RequestSigningRequirement;
import io.jans.shibboleth.model.config.profiles.support.AuthenticationConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.CommonConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.SamlConfigurationSupport;

import java.time.Duration;

public class Saml2SsoProfileConfiguration implements CommonConfigurationCapable, AuthenticationConfigurationCapable, SamlConfigurationCapable, 
    Saml2ConfigurationCapable, SamlAssertionConfigurationCapable,Saml2SsoConfigurationCapable {
    
    private static final Duration DEFAULT_PROFILE_ASSERTION_LIFETIME = Duration.ofMinutes(5);

    private final CommonConfigurationSupport commonConfigurationSupport;
    private final AuthenticationConfigurationSupport authenticationConfigurationSupport;
    private final SamlConfigurationSupport samlConfigurationSupport;
    private final Saml2ConfigurationSupport saml2ConfigurationSupport;
    private final SamlAssertionConfigurationSupport samlAssertionConfigurationSupport;
    private final Saml2SsoConfigurationSupport saml2SsoConfigurationSupport;

    private Saml2SsoProfileConfiguration() {

        commonConfigurationSupport = CommonConfigurationSupport.of();
        authenticationConfigurationSupport = AuthenticationConfigurationSupport.of();
        samlConfigurationSupport = SamlConfigurationSupport.of(MessageSigningPolicy.SIGN_RESPONSES_ONLY);

        saml2ConfigurationSupport = Saml2ConfigurationSupport.of(
            RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE,
            EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT,
            NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS
        );

        samlAssertionConfigurationSupport = SamlAssertionConfigurationSupport.of(
            AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS,
            AssertionTimeCondition.INCLUDE_NOT_BEFORE,
            Duration.ofMinutes(5));

        saml2SsoConfigurationSupport = Saml2SsoConfigurationSupport.of (
            AuthenticationResultReusePolicy.ALLOW_REUSE,
            AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS,
            AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES,
            Duration.ofSeconds(0),
            EndpointValidationPolicy.SKIP_VALIDATION_WHEN_REQUEST_SIGNED,
            AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT,
            FriendlyNameRandomizationPolicy.DO_NOT_RANDOMIZE_FRIENDLY_NAME,
            NameIdentifiers.empty(),
            RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS                                                                                                                                                                                  
        );
    }

    public static Saml2SsoProfileConfiguration defaultConfiguration() {

        return new Saml2SsoProfileConfiguration();
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
}
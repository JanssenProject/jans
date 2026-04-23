package io.jans.shibboleth.model.config.profiles;

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
import io.jans.shibboleth.model.config.profiles.support.CommonConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.model.config.profiles.support.SamlConfigurationSupport;

import java.time.Duration;

public class Saml2LogoutProfileConfiguration implements CommonConfigurationCapable, SamlConfigurationCapable, Saml2ConfigurationCapable {
    
    private static final Duration DEFAULT_PROFILE_ASSERTION_LIFETIME = Duration.ofMinutes(5);

    private final CommonConfigurationSupport commonConfigurationSupport;
    private final SamlConfigurationSupport samlConfigurationSupport;
    private final Saml2ConfigurationSupport saml2ConfigurationSupport;

    private Saml2LogoutProfileConfiguration() {

        commonConfigurationSupport = CommonConfigurationSupport.of();
        samlConfigurationSupport = SamlConfigurationSupport.of(MessageSigningPolicy.SIGN_RESPONSES_ONLY);

        saml2ConfigurationSupport = Saml2ConfigurationSupport.of(
            RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE,
            EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT,
            NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS
        );
    }

    public static Saml2LogoutProfileConfiguration defaultConfiguration() {

        return new Saml2LogoutProfileConfiguration();
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_LOGOUT;
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

    
}
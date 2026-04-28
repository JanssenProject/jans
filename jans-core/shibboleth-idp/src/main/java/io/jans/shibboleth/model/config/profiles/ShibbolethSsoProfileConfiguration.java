package io.jans.shibboleth.model.config.profiles;

import io.jans.shibboleth.model.config.profiles.capabilities.*;
import io.jans.shibboleth.model.config.profiles.common.*;
import io.jans.shibboleth.model.config.profiles.support.*;

import java.time.Duration;

public final class ShibbolethSsoProfileConfiguration implements CommonConfigurationCapable, AuthenticationConfigurationCapable, 
    SamlConfigurationCapable, SamlAssertionConfigurationCapable {
    
    private static final Duration DEFAULT_PROFILE_ASSERTION_LIFETIME = Duration.ofMinutes(5);

    private final CommonConfigurationSupport commonConfigurationSupport;
    private final AuthenticationConfigurationSupport authenticationConfigurationSupport;
    private final SamlConfigurationSupport samlConfigurationSupport;
    private final SamlAssertionConfigurationSupport samlAssertionConfigurationSupport;

    private final AttributeStatementPolicy attributeStatementPolicy;
    private final NameIdentifiers nameIdFormatPrecedence;

    private ShibbolethSsoProfileConfiguration() {
        
       commonConfigurationSupport = CommonConfigurationSupport.of();
       authenticationConfigurationSupport = AuthenticationConfigurationSupport.of(null,null,Duration.ofMinutes(0));
       samlConfigurationSupport = SamlConfigurationSupport.of(MessageSigningPolicy.SIGN_RESPONSES_ONLY);
       samlAssertionConfigurationSupport = SamlAssertionConfigurationSupport.of(
            AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS,
            AssertionTimeCondition.INCLUDE_NOT_BEFORE,
            DEFAULT_PROFILE_ASSERTION_LIFETIME
        );
        attributeStatementPolicy = AttributeStatementPolicy.OMIT_ATTRIBUTE_STATEMENT;
        nameIdFormatPrecedence   = NameIdentifiers.empty();
    }

    public static ShibbolethSsoProfileConfiguration defaultConfiguration() {

       return new ShibbolethSsoProfileConfiguration();
    }

    //Profile Configuration
    @Override
    public ProfileType getType() {

        return ProfileType.SHIBBOLETH_SSO;

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

    //Authentication Configuration
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

    // saml configuration 
    @Override
    public MessageSigningPolicy getMessageSigningPolicy() {

        return samlConfigurationSupport.getMessageSigningPolicy();
    }

    //saml assertion configuration 
    @Override
    public AssertionTimeCondition getAssertionTimeCondition () {

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

    public AttributeStatementPolicy getAttributeStatementPolicy() {

        return attributeStatementPolicy;
    }

    public NameIdentifiers getNameIdFormatPrecedence() {

        return nameIdFormatPrecedence;
    }
}
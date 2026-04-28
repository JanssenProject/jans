package io.jans.shibboleth.model.config.profiles.support;

import io.jans.shibboleth.model.config.profiles.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AttributeStatementPolicy;
import io.jans.shibboleth.model.config.profiles.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.model.config.profiles.common.EndpointValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdentifiers;
import io.jans.shibboleth.model.config.profiles.common.RequestSigningRequirement;

import java.time.Duration;


public class Saml2SsoConfigurationSupport {
    
    private final AuthenticationResultReusePolicy authenticationReuseResultPolicy;
    private final AssertionEncryptionPolicy assertionEncryptionPolicy;
    private final AttributeEncryptionPolicy attributeEncryptionPolicy;
    private final Duration maximumSPSessionLifetime;
    private final EndpointValidationPolicy endpointValidationPolicy;
    private final AttributeStatementPolicy attributeStatementPolicy;
    private final FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;
    private final NameIdentifiers nameIdFormatPrecedence;
    private final RequestSigningRequirement requestSigningRequirement;

    private Saml2SsoConfigurationSupport (
        AuthenticationResultReusePolicy authenticationResultReusePolicy,
        AssertionEncryptionPolicy assertionEncryptionPolicy,
        AttributeEncryptionPolicy attributeEncryptionPolicy,
        Duration maximumSPSessionLifetime,
        EndpointValidationPolicy endpointValidationPolicy,
        AttributeStatementPolicy attributeStatementPolicy,
        FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy,
        NameIdentifiers nameIdFormatPrecedence,
        RequestSigningRequirement requestSigningRequirement ) {

        
        this.authenticationReuseResultPolicy = authenticationResultReusePolicy!=null ? authenticationResultReusePolicy : AuthenticationResultReusePolicy.ALLOW_REUSE;
        this.assertionEncryptionPolicy = assertionEncryptionPolicy!=null ? assertionEncryptionPolicy : AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS;
        this.attributeEncryptionPolicy = attributeEncryptionPolicy!=null ? attributeEncryptionPolicy: AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES;
        this.maximumSPSessionLifetime  = maximumSPSessionLifetime!=null ? maximumSPSessionLifetime : Duration.ofSeconds(0);
        this.endpointValidationPolicy  = endpointValidationPolicy!=null ? endpointValidationPolicy : EndpointValidationPolicy.SKIP_VALIDATION_WHEN_REQUEST_SIGNED;
        this.attributeStatementPolicy  = attributeStatementPolicy!=null ? attributeStatementPolicy : AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT;
        this.friendlyNameRandomizationPolicy = friendlyNameRandomizationPolicy!=null ? friendlyNameRandomizationPolicy : FriendlyNameRandomizationPolicy.RANDOMIZED;
        this.nameIdFormatPrecedence = nameIdFormatPrecedence!=null ? nameIdFormatPrecedence : NameIdentifiers.empty();
        this.requestSigningRequirement = requestSigningRequirement!=null ? requestSigningRequirement : RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS;
    }

    public static final Saml2SsoConfigurationSupport of (
        AuthenticationResultReusePolicy authenticationReuseResultPolicy,
        AssertionEncryptionPolicy assertionEncryptionPolicy,
        AttributeEncryptionPolicy attributeEncryptionPolicy,
        Duration maximumSPSessionLifetime,
        EndpointValidationPolicy endpointValidationPolicy,
        AttributeStatementPolicy attributeStatementPolicy,
        FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy,
        NameIdentifiers nameIdFormatPrecedence,
        RequestSigningRequirement requestSigningRequirement) {
        
        return new Saml2SsoConfigurationSupport(
            authenticationReuseResultPolicy,assertionEncryptionPolicy,attributeEncryptionPolicy,
            maximumSPSessionLifetime,endpointValidationPolicy,attributeStatementPolicy,
            friendlyNameRandomizationPolicy,nameIdFormatPrecedence,requestSigningRequirement);
    }
    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationReuseResultPolicy;
    }

    public AssertionEncryptionPolicy getAssertionEncryptionPolicy() {

        return assertionEncryptionPolicy;
    }

    public AttributeEncryptionPolicy getAttributeEncryptionPolicy() {

        return attributeEncryptionPolicy;
    }

    public Duration getMaximumSPSessionLifetime() {

        return maximumSPSessionLifetime;
    }

    public EndpointValidationPolicy getEndpointValidationPolicy() {

        return endpointValidationPolicy;
    }

    public AttributeStatementPolicy getAttributeStatementPolicy() {

        return attributeStatementPolicy;
    }

    public FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy() {

        return friendlyNameRandomizationPolicy;
    }

    public NameIdentifiers getNameIdFormatPrecedence() {

        return nameIdFormatPrecedence;
    }

    public RequestSigningRequirement getRequestSigningRequirement() {

        return requestSigningRequirement;
    }

    
}
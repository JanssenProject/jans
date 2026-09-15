package io.jans.shibboleth.trust.config.profile;

import java.time.Duration;

import io.jans.shibboleth.trust.config.profile.common.*;

public class SamlProfileConfigurationDefaults {
    
    private SamlProfileConfigurationDefaults() {
        //empty private constructory
    }

    public static ShibbolethSsoProfileConfiguration shibbolethSso() {
        
        return ShibbolethSsoProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .postAuthenticationFlows(InterceptorFlows.empty())
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE)
            .maximumAuthenticationAge(Duration.ofMinutes(0))
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .assertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE)
            .assertionLifetime(Duration.ofMinutes(5))
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .attributeStatementPolicy(AttributeStatementPolicy.OMIT_ATTRIBUTE_STATEMENT)
            .nameIdFormatPrecedence(NameIdentifiers.empty())
            .build()
            .getValue();
    }

    public static Saml2AttributeQueryProfileConfiguration saml2AttributeQuery() {
        
        return Saml2AttributeQueryProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .assertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE)
            .assertionLifetime(Duration.ofMinutes(5))
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.DO_NOT_ENCRYPT_ASSERTIONS)
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES)
            .friendlyRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED)
            .build()
            .getValue();
    }

    public static Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolution() {

        return Saml2ArtifactResolutionProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.DO_NOT_ENCRYPT_ASSERTIONS)
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES)
            .build()
            .getValue();
    }

    public static Saml2EcpProfileConfiguration saml2Ecp() {

        return Saml2EcpProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .assertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE)
            .assertionLifetime(Duration.ofMinutes(5))
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE)
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS)
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES)
            .maximumSPSessionLifetime(Duration.ofMinutes(0))
            .endpointValidationPolicy(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT)
            .attributeStatementPolicy((AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT))
            .friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED)
            .nameIdFormatPrecedence(NameIdentifiers.empty())
            .requestSigningRequirement(RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS)
            .build()
            .getValue();
    }

    public static Saml2SsoProfileConfiguration saml2Sso() {

        return Saml2SsoProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .postAuthenticationFlows(InterceptorFlows.empty())
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE)
            .maximumAuthenticationAge(Duration.ofMinutes(0))
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .assertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE)
            .assertionLifetime(Duration.ofMinutes(5))
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS)
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES)
            .maximumSPSessionLifetime(Duration.ofMinutes(0))
            .endpointValidationPolicy(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT)
            .attributeStatementPolicy(AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT)
            .friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED)
            .nameIdFormatPrecedence(NameIdentifiers.empty())
            .requestSigningRequirement(RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS)
            .build()
            .getValue();
    }

    public static Saml2LogoutProfileConfiguration saml2Logout() {

        return Saml2LogoutProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .build()
            .getValue();
    }
}

package io.jans.shibboleth.trust.persistence.config.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Stored JSON representation of a trust relationship's six profile configurations (the {@code jansProfiles}
 * {@code @JsonObject} column). A dedicated persistence type (TP3), independent of the API wire DTOs.
 *
 * <p>Flat per-profile sub-objects: each field maps 1:1 to a profile getter and its builder setter, so the
 * mapper serializes via getters and rebuilds via {@code from(defaults)} + setters. Enums are stored as
 * {@code name()} strings, {@code Duration} as ISO-8601 strings, and interceptor flows / name-id precedence
 * as string lists. Public fields keep this dumb carrier compact; Jackson serializes them directly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfilesPayload {

    public ShibbolethSso shibbolethSso;
    public Saml2Sso saml2Sso;
    public Saml2Ecp saml2Ecp;
    public Saml2AttributeQuery saml2AttributeQuery;
    public Saml2ArtifactResolution saml2ArtifactResolution;
    public Saml2Logout saml2Logout;

    public static class ShibbolethSso {

        public String status;
        public List<String> inboundFlows;
        public List<String> outboundFlows;
        public List<String> postAuthenticationFlows;
        public String maxAuthenticationAge;
        public String authenticationResultReusePolicy;
        public String messageSigningPolicy;
        public String assertionTimeCondition;
        public String assertionLifetime;
        public String assertionSigningPolicy;
        public String attributeStatementPolicy;
        public List<String> nameIdFormatPrecedence;
    }

    public static class Saml2Sso {

        public String status;
        public List<String> inboundFlows;
        public List<String> outboundFlows;
        public List<String> postAuthenticationFlows;
        public String maxAuthenticationAge;
        public String authenticationResultReusePolicy;
        public String messageSigningPolicy;
        public String requestSignatureValidationPolicy;
        public String encryptionFallbackPolicy;
        public String nameIdEncryptionPolicy;
        public String assertionTimeCondition;
        public String assertionLifetime;
        public String assertionSigningPolicy;
        public String assertionEncryptionPolicy;
        public String attributeEncryptionPolicy;
        public String maximumSpSessionLifetime;
        public String endpointValidationPolicy;
        public String attributeStatementPolicy;
        public String friendlyNameRandomizationPolicy;
        public List<String> nameIdFormatPrecedence;
        public String requestSigningRequirement;
    }

    public static class Saml2Ecp {

        public String status;
        public List<String> inboundFlows;
        public List<String> outboundFlows;
        public String messageSigningPolicy;
        public String requestSignatureValidationPolicy;
        public String encryptionFallbackPolicy;
        public String nameIdEncryptionPolicy;
        public String assertionTimeCondition;
        public String assertionLifetime;
        public String assertionSigningPolicy;
        public String authenticationResultReusePolicy;
        public String assertionEncryptionPolicy;
        public String attributeEncryptionPolicy;
        public String maximumSpSessionLifetime;
        public String endpointValidationPolicy;
        public String attributeStatementPolicy;
        public String friendlyNameRandomizationPolicy;
        public List<String> nameIdFormatPrecedence;
        public String requestSigningRequirement;
    }

    public static class Saml2AttributeQuery {

        public String status;
        public List<String> inboundFlows;
        public List<String> outboundFlows;
        public String messageSigningPolicy;
        public String assertionTimeCondition;
        public String assertionLifetime;
        public String assertionSigningPolicy;
        public String requestSignatureValidationPolicy;
        public String encryptionFallbackPolicy;
        public String nameIdEncryptionPolicy;
        public String assertionEncryptionPolicy;
        public String attributeEncryptionPolicy;
        public String friendlyNameRandomizationPolicy;
    }

    public static class Saml2ArtifactResolution {

        public String status;
        public List<String> inboundFlows;
        public List<String> outboundFlows;
        public String messageSigningPolicy;
        public String requestSignatureValidationPolicy;
        public String encryptionFallbackPolicy;
        public String nameIdEncryptionPolicy;
        public String assertionEncryptionPolicy;
        public String attributeEncryptionPolicy;
        public String assertionSigningPolicy;
    }

    public static class Saml2Logout {

        public String status;
        public List<String> inboundFlows;
        public List<String> outboundFlows;
        public String messageSigningPolicy;
        public String requestSignatureValidationPolicy;
        public String encryptionFallbackPolicy;
        public String nameIdEncryptionPolicy;
    }
}

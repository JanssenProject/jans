package io.jans.as.model.ssa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Z
 */
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class SsaValidationConfig {

    private String id;
    private SsaValidationType type;
    private String displayName;
    private String description;
    private List<String> scopes;
    private List<String> allowedClaims;
    private String jwks;
    private String jwksUri;
    private List<String> issuers;
    private String configurationEndpoint;
    private String configurationEndpointClaim;
    private String sharedSecret;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SsaValidationType getType() {
        return type;
    }

    public void setType(SsaValidationType type) {
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getScopes() {
        if (scopes == null) scopes = new ArrayList<>();
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> getAllowedClaims() {
        if (allowedClaims == null) allowedClaims = new ArrayList<>();
        return allowedClaims;
    }

    public void setAllowedClaims(List<String> allowedClaims) {
        this.allowedClaims = allowedClaims;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public List<String> getIssuers() {
        if (issuers == null) issuers = new ArrayList<>();
        return issuers;
    }

    public void setIssuers(List<String> issuers) {
        this.issuers = issuers;
    }

    public String getConfigurationEndpoint() {
        return configurationEndpoint;
    }

    public void setConfigurationEndpoint(String configurationEndpoint) {
        this.configurationEndpoint = configurationEndpoint;
    }

    public String getConfigurationEndpointClaim() {
        return configurationEndpointClaim;
    }

    public void setConfigurationEndpointClaim(String configurationEndpointClaim) {
        this.configurationEndpointClaim = configurationEndpointClaim;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    @Override
    public String toString() {
        return "SsaValidationConfig{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", scopes=" + scopes +
                ", allowedClaims=" + allowedClaims +
                ", jwks='" + jwks + '\'' +
                ", jwksUri='" + jwksUri + '\'' +
                ", issuers=" + issuers +
                ", configurationEndpoint='" + configurationEndpoint + '\'' +
                ", configurationEndpointClaim='" + configurationEndpointClaim + '\'' +
                ", sharedSecret='" + sharedSecret + '\'' +
                '}';
    }
}

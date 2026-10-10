package io.jans.as.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrustedIssuerConfig implements Serializable {

    @JsonProperty("automaticallyGrantedScopes")
    private List<String> automaticallyGrantedScopes = new ArrayList<>();

    @JsonProperty("jwks")
    private String jwks;

    @JsonProperty("jwksUri")
    private String jwksUri;

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

    public List<String> getAutomaticallyGrantedScopes() {
        if (automaticallyGrantedScopes == null) automaticallyGrantedScopes = new ArrayList<>();
        return automaticallyGrantedScopes;
    }

    public void setAutomaticallyGrantedScopes(List<String> automaticallyGrantedScopes) {
        this.automaticallyGrantedScopes = automaticallyGrantedScopes;
    }

    @Override
    public String toString() {
        return "TrustedIssuerConfig{" +
                "automaticallyGrantedScopes=" + automaticallyGrantedScopes +
                ", jwksUri='" + jwksUri + '\'' +
                '}';
    }
}

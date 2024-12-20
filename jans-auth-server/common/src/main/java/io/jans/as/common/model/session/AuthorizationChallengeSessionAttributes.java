package io.jans.as.common.model.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Z
 */
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class AuthorizationChallengeSessionAttributes implements Serializable {

    @JsonProperty("acr_values")
    private String acrValues;

    // jkt - JWK SHA-256 Thumbprint confirmation method.
    // The value of the jkt member MUST be the base64url encoding (as defined in [RFC7515]) of the JWK SHA-256 Thumbprint
    // (according to [RFC7638]) of the DPoP public key (in JWK format) to which the access token is bound.
    @JsonProperty("jkt")
    private String jkt;

    @JsonProperty("attributes")
    private Map<String, String> attributes;

    public Map<String, String> getAttributes() {
        if (attributes == null) attributes = new HashMap<>();
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getJkt() {
        return jkt;
    }

    public AuthorizationChallengeSessionAttributes setJkt(String jkt) {
        this.jkt = jkt;
        return this;
    }

    @Override
    public String toString() {
        return "DeviceSessionAttributes{" +
                "acrValues='" + acrValues + '\'' +
                "attributes='" + attributes + '\'' +
                "jkt='" + jkt + '\'' +
                '}';
    }
}

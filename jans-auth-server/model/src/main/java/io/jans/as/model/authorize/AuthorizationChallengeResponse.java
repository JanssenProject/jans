package io.jans.as.model.authorize;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationChallengeResponse {

    @JsonProperty(value = "authorization_code")
    private String authorizationCode;

    /**
     * Gets authorization code
     *
     * @return authorization code
     */
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    /**
     * Sets authorization code
     *
     * @param authorizationCode authorization code
     */
    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    /**
     * Returns string representation of authorization challenge response
     *
     * @return string representation of authorization challenge response
     */
    @Override
    public String toString() {
        return "AuthorizationChallengeResponse{" +
                "authorizationCode='" + authorizationCode + '\'' +
                '}';
    }
}

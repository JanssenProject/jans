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
    @JsonProperty(value = "logout_status_jwt")
    private String logoutStatusJwt;

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
     * Gets logout status jwt
     *
     * @return logout status jwt
     */
    public String getLogoutStatusJwt() {
        return logoutStatusJwt;
    }

    /**
     * Sets logout status jwt
     *
     * @param logoutStatusJwt logout status jwt
     * @return authorization challenge response
     */
    public AuthorizationChallengeResponse setLogoutStatusJwt(String logoutStatusJwt) {
        this.logoutStatusJwt = logoutStatusJwt;
        return this;
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
                "logoutStatusJwt='" + logoutStatusJwt + '\'' +
                '}';
    }
}

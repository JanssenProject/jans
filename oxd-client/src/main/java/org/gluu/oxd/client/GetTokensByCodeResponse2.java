package org.gluu.oxd.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.gluu.oxd.common.response.IOpResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
public class GetTokensByCodeResponse2  implements IOpResponse {

    @JsonProperty(value = "access_token")
    private String accessToken;
    @JsonProperty(value = "expires_in")
    private int expiresIn; // expiration time in seconds
    @JsonProperty(value = "id_token")
    private String idToken;
    @JsonProperty(value = "refresh_token")
    private String refreshToken;
    @JsonProperty("id_token_claims")
    private JsonNode idTokenClaims;

    public GetTokensByCodeResponse2() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public JsonNode getIdTokenClaims() {
        return idTokenClaims;
    }

    public void setIdTokenClaims(JsonNode idTokenClaims) {
        this.idTokenClaims = idTokenClaims;
    }

    @Override
    public String toString() {
        return "GetTokensByCodeResponse2{" +
                "accessToken='" + accessToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", idToken='" + idToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", idTokenClaims=" + idTokenClaims +
                '}';
    }
}

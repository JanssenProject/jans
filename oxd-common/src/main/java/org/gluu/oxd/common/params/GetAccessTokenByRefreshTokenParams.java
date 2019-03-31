package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAccessTokenByRefreshTokenParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "refresh_token")
    private String refresh_token;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;

    @Override
    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setRefreshToken(String refreshToken) {
        this.refresh_token = refreshToken;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "GetAccessTokenByRefreshTokenParams{" +
                "oxd_id='" + oxd_id + '\'' +
                ", refresh_token='" + refresh_token + '\'' +
                ", scope=" + scope +
                '}';
    }
}

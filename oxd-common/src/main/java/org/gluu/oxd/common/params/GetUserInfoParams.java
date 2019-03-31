package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetUserInfoParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "access_token")
    private String access_token;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public GetUserInfoParams() {
    }

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String accessToken) {
        this.access_token = accessToken;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetUserInfoParams that = (GetUserInfoParams) o;

        if (access_token != null ? !access_token.equals(that.access_token) : that.access_token != null) return false;
        return !(oxd_id != null ? !oxd_id.equals(that.oxd_id) : that.oxd_id != null);

    }

    @Override
    public int hashCode() {
        int result = oxd_id != null ? oxd_id.hashCode() : 0;
        result = 31 * result + (access_token != null ? access_token.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetUserInfoParams");
        sb.append("{access_token='").append(access_token).append('\'');
        sb.append(", oxd_id='").append(oxd_id).append('\'');
        sb.append(", protection_access_token='").append(protection_access_token).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

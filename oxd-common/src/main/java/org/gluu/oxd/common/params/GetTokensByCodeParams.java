package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetTokensByCodeParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "code")
    private String code;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public GetTokensByCodeParams() {
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetTokensByCodeParams that = (GetTokensByCodeParams) o;

        return !(code != null ? !code.equals(that.code) : that.code != null) && !(oxd_id != null ? !oxd_id.equals(that.oxd_id) : that.oxd_id != null);
    }

    @Override
    public int hashCode() {
        int result = oxd_id != null ? oxd_id.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetTokensByCodeParams");
        sb.append("{code='").append(code).append('\'');
        sb.append(", oxd_id='").append(oxd_id).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", protection_access_token='").append(protection_access_token).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

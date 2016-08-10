package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetUserInfoParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "access_token")
    private String accessToken;

    public GetUserInfoParams() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GetUserInfoParams that = (GetUserInfoParams) o;

        if (accessToken != null ? !accessToken.equals(that.accessToken) : that.accessToken != null) return false;
        return !(oxdId != null ? !oxdId.equals(that.oxdId) : that.oxdId != null);

    }

    @Override
    public int hashCode() {
        int result = oxdId != null ? oxdId.hashCode() : 0;
        result = 31 * result + (accessToken != null ? accessToken.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetUserInfoParams");
        sb.append("{accessToken='").append(accessToken).append('\'');
        sb.append(", oxdId='").append(oxdId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

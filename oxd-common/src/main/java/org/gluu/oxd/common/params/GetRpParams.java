package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRpParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "list")
    private Boolean list;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public GetRpParams() {
    }

    public GetRpParams(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public Boolean getList() {
        return list;
    }

    public void setList(Boolean list) {
        this.list = list;
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    @Override
    public String toString() {
        return "GetRpParams{" +
                "oxdId='" + oxd_id + '\'' +
                "list='" + list + '\'' +
                '}';
    }
}

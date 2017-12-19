package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoveSiteParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;

    public RemoveSiteParams() {
    }

    public RemoveSiteParams(String oxdId) {
        this(oxdId, null);
    }

    public RemoveSiteParams(String oxdId, String protectionAccessToken) {
        this.oxdId = oxdId;
        this.protectionAccessToken = protectionAccessToken;
    }

    @Override
    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    @Override
    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
    }

    @Override
    public String toString() {
        return "RemoveSiteParams{" +
                "oxdId='" + oxdId + '\'' +
                ", protectionAccessToken='" + protectionAccessToken + '\'' +
                '}';
    }
}

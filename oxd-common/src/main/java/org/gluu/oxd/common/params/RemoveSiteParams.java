package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoveSiteParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public RemoveSiteParams() {
    }

    public RemoveSiteParams(String oxdId) {
        this(oxdId, null);
    }

    public RemoveSiteParams(String oxdId, String protectionAccessToken) {
        this.oxd_id = oxdId;
        this.protection_access_token = protectionAccessToken;
    }

    @Override
    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    @Override
    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    @Override
    public String toString() {
        return "RemoveSiteParams{" +
                "oxd_id='" + oxd_id + '\'' +
                ", protection_access_token='" + protection_access_token + '\'' +
                '}';
    }
}

package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntrospectRptParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "rpt")
    private String rpt;
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;

    @Override
    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    @Override
    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
    }
}

package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpAuthorizeRptParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "rpt")
    private String rpt;
    @JsonProperty(value = "ticket")
    private String ticket;
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

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RpAuthorizeRptParams");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append(", rpt='").append(rpt).append('\'');
        sb.append(", ticket='").append(ticket).append('\'');
        sb.append(", protectionAccessToken='").append(protectionAccessToken).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpGetRptParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "ticket")
    private String ticket;
    @JsonProperty(value = "claim_token")
    private String claimToken;
    @JsonProperty(value = "claim_token_format")
    private String claimTokenFormat;
    @JsonProperty(value = "pct")
    private String pct;
    @JsonProperty(value = "rpt")
    private String rpt;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;

    public RpGetRptParams() {
    }

    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }


    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getClaimToken() {
        return claimToken;
    }

    public void setClaimToken(String claimToken) {
        this.claimToken = claimToken;
    }

    public String getClaimTokenFormat() {
        return claimTokenFormat;
    }

    public void setClaimTokenFormat(String claimTokenFormat) {
        this.claimTokenFormat = claimTokenFormat;
    }

    public String getPct() {
        return pct;
    }

    public void setPct(String pct) {
        this.pct = pct;
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "RpGetRptParams{" +
                "oxdId='" + oxdId + '\'' +
                ", ticket='" + ticket + '\'' +
                ", claimToken='" + claimToken + '\'' +
                ", claimTokenFormat='" + claimTokenFormat + '\'' +
                ", pct='" + pct + '\'' +
                ", rpt='" + rpt + '\'' +
                ", scope=" + scope +
                ", state='" + state + '\'' +
                ", protectionAccessToken='" + protectionAccessToken + '\'' +
                '}';
    }
}

/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RpGetRptParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "ticket")
    private String ticket;
    @JsonProperty(value = "claim_token")
    private String claim_token;
    @JsonProperty(value = "claim_token_format")
    private String claim_token_format;
    @JsonProperty(value = "pct")
    private String pct;
    @JsonProperty(value = "rpt")
    private String rpt;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public RpGetRptParams() {
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }


    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getClaimToken() {
        return claim_token;
    }

    public void setClaimToken(String claimToken) {
        this.claim_token = claimToken;
    }

    public String getClaimTokenFormat() {
        return claim_token_format;
    }

    public void setClaimTokenFormat(String claimTokenFormat) {
        this.claim_token_format = claimTokenFormat;
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
                "oxd_id='" + oxd_id + '\'' +
                ", ticket='" + ticket + '\'' +
                ", claim_token='" + claim_token + '\'' +
                ", claim_token_format='" + claim_token_format + '\'' +
                ", pct='" + pct + '\'' +
                ", rpt='" + rpt + '\'' +
                ", scope=" + scope +
                ", state='" + state + '\'' +
                ", protection_access_token='" + protection_access_token + '\'' +
                '}';
    }
}

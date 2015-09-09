/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class AuthorizeRptParams implements IParams {

    @JsonProperty(value = "aat_token")
    private String aatToken;
    @JsonProperty(value = "rpt_token")
    private String rptToken;
    @JsonProperty(value = "am_host")
    private String amHost;
    @JsonProperty(value = "ticket")
    private String ticket;
    @JsonProperty(value = "claims")
    private Map<String, List<String>> claims;

    public AuthorizeRptParams() {
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> p_claims) {
        claims = p_claims;
    }

    public String getAatToken() {
        return aatToken;
    }

    public void setAatToken(String p_aatToken) {
        aatToken = p_aatToken;
    }

    public String getAmHost() {
        return amHost;
    }

    public void setAmHost(String p_amHost) {
        amHost = p_amHost;
    }

    public String getRptToken() {
        return rptToken;
    }

    public void setRptToken(String p_rptToken) {
        rptToken = p_rptToken;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String p_ticket) {
        ticket = p_ticket;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AuthorizeRptParams");
        sb.append("{aatToken='").append(aatToken).append('\'');
        sb.append(", rptToken='").append(rptToken).append('\'');
        sb.append(", amHost='").append(amHost).append('\'');
        sb.append(", ticket='").append(ticket).append('\'');
        sb.append(", claims='").append(claims).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request for getting token status
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *         Date: 10/23/2012
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"rpt", "ticket", "claim_tokens"})
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class RptAuthorizationRequest {

    private String rpt;
    private String ticket;
    private ClaimTokenList claims;

    public RptAuthorizationRequest() {
    }

    public RptAuthorizationRequest(String rpt, String ticket) {
        this.rpt = rpt;
        this.ticket = ticket;
    }

    @JsonProperty(value = "claim_tokens")
    @XmlElement(name = "claim_tokens")
    public ClaimTokenList getClaims() {
        return claims;
    }

    public void setClaims(ClaimTokenList p_claims) {
        claims = p_claims;
    }

    @JsonProperty(value = "rpt")
    @XmlElement(name = "rpt")
    public String getRpt() {
        return rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    @JsonProperty(value = "ticket")
    @XmlElement(name = "ticket")
    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RptAuthorizationRequest");
        sb.append("{claims=").append(claims);
        sb.append(", rpt='").append(rpt).append('\'');
        sb.append(", ticket='").append(ticket).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

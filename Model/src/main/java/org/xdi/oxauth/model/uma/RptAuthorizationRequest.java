package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request for getting token status
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *         Date: 10/23/2012
 */
@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"rpt", "ticket", "claims"})
@XmlRootElement
public class RptAuthorizationRequest {

    private String rpt;
    private String ticket;
    private Map<String, List<String>> claims = new HashMap<String, List<String>>();

    public RptAuthorizationRequest() {
    }

    public RptAuthorizationRequest(String rpt, String ticket) {
        this.rpt = rpt;
        this.ticket = ticket;
    }

    @JsonProperty(value = "claims")
    @XmlElement(name = "claims")
    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> p_claims) {
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

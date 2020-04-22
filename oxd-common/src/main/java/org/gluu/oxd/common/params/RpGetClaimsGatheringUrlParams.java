package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/06/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpGetClaimsGatheringUrlParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "ticket")
    private String ticket;
    @JsonProperty(value = "claims_redirect_uri")
    private String claims_redirect_uri;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "custom_parameters")
    private Map<String, String> custom_parameters;

    public RpGetClaimsGatheringUrlParams() {
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

    public String getClaimsRedirectUri() {
        return claims_redirect_uri;
    }

    public void setClaimsRedirectUri(String claimsRedirectUri) {
        this.claims_redirect_uri = claimsRedirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Map<String, String> getCustomParameters() {
        return custom_parameters;
    }

    public void setCustomParameters(Map<String, String> custom_parameters) {
        this.custom_parameters = custom_parameters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RpGetGetClaimsGatheringUrlParams");
        sb.append("{oxd_id='").append(oxd_id).append('\'');
        sb.append(", ticket=").append(ticket);
        sb.append(", claims_redirect_uri=").append(claims_redirect_uri);
        sb.append(", state=").append(state);
        sb.append(", custom_parameters=").append(custom_parameters);
        sb.append('}');
        return sb.toString();
    }
}

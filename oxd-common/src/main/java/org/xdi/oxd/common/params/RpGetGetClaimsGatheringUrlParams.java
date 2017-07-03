package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/06/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpGetGetClaimsGatheringUrlParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "ticket")
    private String ticket;
    @JsonProperty(value = "claims_redirect_uri")
    private String claimsRedirectUri;
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;

    public RpGetGetClaimsGatheringUrlParams() {
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

    public String getClaimsRedirectUri() {
        return claimsRedirectUri;
    }

    public void setClaimsRedirectUri(String claimsRedirectUri) {
        this.claimsRedirectUri = claimsRedirectUri;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RpGetGetClaimsGatheringUrlParams");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append(", ticket=").append(ticket);
        sb.append(", claimsRedirectUri=").append(claimsRedirectUri);
        sb.append(", protectionAccessToken=").append(protectionAccessToken);
        sb.append('}');
        return sb.toString();
    }
}

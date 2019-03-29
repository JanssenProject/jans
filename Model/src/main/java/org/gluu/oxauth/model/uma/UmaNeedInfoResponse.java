package org.gluu.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonProperty;
import org.gluu.model.uma.ClaimDefinition;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class UmaNeedInfoResponse implements Serializable {

    @JsonProperty(value = "error")
    private String error;
    @JsonProperty(value = "required_claims")
    private List<ClaimDefinition> requiredClaims;
    @JsonProperty(value = "redirect_user")
    private String redirectUser;
    @JsonProperty(value = "ticket")
    private String ticket;

    public UmaNeedInfoResponse() {
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getRedirectUser() {
        return redirectUser;
    }

    public void setRedirectUser(String redirectUser) {
        this.redirectUser = redirectUser;
    }

    public List<ClaimDefinition> getRequiredClaims() {
        return requiredClaims;
    }

    public void setRequiredClaims(List<ClaimDefinition> requiredClaims) {
        this.requiredClaims = requiredClaims;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    /**
     * Builds GET claims-gathering url. Note: it is strictly recommended to use POST http method.
     *
     * @return claims-gathering url for GET request.
     */
    public String buildClaimsGatheringUrl(String clientId, String claimsRedirectUri) {
        String result = redirectUser;
        result += result.contains("?") ? "&" : "?";
        result += "client_id=" + clientId;
        result += "&ticket=" + ticket;
        result += "&claims_redirect_uri=" + claimsRedirectUri;
        return result;
    }
}

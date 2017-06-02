package org.xdi.oxauth.uma.authorization;

import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.model.uma.ClaimDefinition;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class NeedInfoError implements Serializable {

    @JsonProperty(value = "required_claims")
    private List<ClaimDefinition> requiredClaims;
    @JsonProperty(value = "redirect_user")
    private boolean redirectUser;
    @JsonProperty(value = "ticket")
    private String ticket;

    public NeedInfoError() {
    }

    public boolean isRedirectUser() {
        return redirectUser;
    }

    public void setRedirectUser(boolean redirectUser) {
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
}

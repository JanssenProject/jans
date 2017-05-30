package org.xdi.oxauth.uma.authorization;

import java.io.Serializable;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class NeedInfoRequestingPartyClaims implements Serializable {

    @JsonProperty(value = "required_claims")
    private List<RequiredClaim> requiredClaims;
    @JsonProperty(value = "redirect_user")
    private boolean redirectUser;
    @JsonProperty(value = "ticket")
    private String ticket;

    public NeedInfoRequestingPartyClaims() {
    }

    public boolean isRedirectUser() {
        return redirectUser;
    }

    public void setRedirectUser(boolean redirectUser) {
        this.redirectUser = redirectUser;
    }

    public List<RequiredClaim> getRequiredClaims() {
        return requiredClaims;
    }

    public void setRequiredClaims(List<RequiredClaim> requiredClaims) {
        this.requiredClaims = requiredClaims;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}

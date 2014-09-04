package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class RegisterPermissionTicketOpResponse implements IOpResponse {

    @JsonProperty(value = "ticket")
    private String ticket;

    public RegisterPermissionTicketOpResponse() {
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String p_ticket) {
        ticket = p_ticket;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterPermissionTicketOpResponse");
        sb.append("{ticket='").append(ticket).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

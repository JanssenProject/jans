package org.gluu.oxd.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 10/06/2016
 */

public class RsCheckAccessResponse implements IOpResponse {

    @JsonProperty(value = "access")
    private String access;
    @JsonProperty(value = "www-authenticate_header")
    private String wwwAuthenticateHeader;
    @JsonProperty(value = "ticket")
    private String ticket;

    public RsCheckAccessResponse() {
    }

    public RsCheckAccessResponse(String access) {
        this.access = access;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getWwwAuthenticateHeader() {
        return wwwAuthenticateHeader;
    }

    public void setWwwAuthenticateHeader(String wwwAuthenticateHeader) {
        this.wwwAuthenticateHeader = wwwAuthenticateHeader;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RsCheckResponse");
        sb.append("{access='").append(access).append('\'');
        sb.append(", wwwAuthenticateHeader='").append(wwwAuthenticateHeader).append('\'');
        sb.append(", ticket='").append(ticket).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

package io.jans.ca.common.response;

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

    @JsonProperty(value = "error")
    private String error;

    @JsonProperty(value = "error_description")
    private String errorDescription;

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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RsCheckResponse");
        sb.append("{access='").append(access).append('\'');
        sb.append(", wwwAuthenticateHeader='").append(wwwAuthenticateHeader).append('\'');
        sb.append(", error='").append(error).append('\'');
        sb.append(", errorDescription='").append(errorDescription).append('\'');
        sb.append(", ticket='").append(ticket).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

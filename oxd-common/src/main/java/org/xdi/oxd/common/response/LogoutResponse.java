package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

public class LogoutResponse implements IOpResponse {

    @JsonProperty(value = "uri")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "uri")
    private String uri;

    public LogoutResponse() {
    }

    public LogoutResponse(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LogoutResponse");
        sb.append("{uri='").append(uri).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

public class LogoutResponse implements IOpResponse {

    @JsonProperty(value = "html")
    private String html;

    public LogoutResponse() {
    }

    public LogoutResponse(String html) {
        this.html = html;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LogoutResponse");
        sb.append("{html='").append(html).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

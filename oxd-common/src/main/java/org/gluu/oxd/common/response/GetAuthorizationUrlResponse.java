package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/10/2015
 */

public class GetAuthorizationUrlResponse implements IOpResponse {

    @JsonProperty(value = "authorization_url")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorization_url")
    private String authorizationUrl;

    public GetAuthorizationUrlResponse() {
    }

    public GetAuthorizationUrlResponse(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetAuthorizationUrlResponse");
        sb.append("{authorizationUrl='").append(authorizationUrl).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

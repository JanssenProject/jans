package org.gluu.oxd.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetRequestUriResponse implements IOpResponse {

    @JsonProperty(value = "request_uri" )
    private String requestUri;

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }
}

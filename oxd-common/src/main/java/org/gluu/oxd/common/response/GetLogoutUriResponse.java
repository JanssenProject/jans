package org.gluu.oxd.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/11/2015
 */

public class GetLogoutUriResponse implements IOpResponse {

    @JsonProperty(value = "uri")
    private String uri;

    public GetLogoutUriResponse() {
    }

    public GetLogoutUriResponse(String uri) {
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
        return "GetLogoutUriResponse" +
                "{uri='" + uri + '\'' +
                '}';
    }
}

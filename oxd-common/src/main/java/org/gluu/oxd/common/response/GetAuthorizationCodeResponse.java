package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetAuthorizationCodeResponse implements IOpResponse {

    @JsonProperty(value = "code")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "code")
    private String code;

    public GetAuthorizationCodeResponse() {
    }

    public GetAuthorizationCodeResponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GetAuthorizationCodeResponse");
        sb.append("{code='").append(code).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

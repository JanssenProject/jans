package org.gluu.oxd.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetRequestObjectJwtResponse implements IOpResponse {

    @JsonProperty(value = "request_object" )
    private String requestObject;

    public String getRequestObject() {
        return requestObject;
    }

    public void setRequestObject(String requestObject) {
        this.requestObject = requestObject;
    }

}
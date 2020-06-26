package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRequestObjectJwtParams implements IParams {

    @JsonProperty(value = "request_object_id")
    private String request_object_id;

    public String getRequestObjectId() {
        return request_object_id;
    }

    public void setRequestObjectId(String request_object_id) {
        this.request_object_id = request_object_id;
    }

    @Override
    public String toString() {
        return "GetRequestObjectJwtParams{" +
                "request_object_id='" + request_object_id + '\'' +
                '}';
    }
}

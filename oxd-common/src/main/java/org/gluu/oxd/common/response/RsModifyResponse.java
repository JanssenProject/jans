package org.gluu.oxd.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RsModifyResponse implements IOpResponse {
    @JsonProperty(value = "oxd_id")
    private String oxdId;

    public RsModifyResponse() {
    }

    public RsModifyResponse(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }
}

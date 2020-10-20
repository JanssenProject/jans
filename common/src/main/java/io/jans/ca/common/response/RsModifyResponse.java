package io.jans.ca.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RsModifyResponse implements IOpResponse {
    @JsonProperty(value = "rp_id")
    private String rpId;

    public RsModifyResponse() {
    }

    public RsModifyResponse(String rpId) {
        this.rpId = rpId;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }
}

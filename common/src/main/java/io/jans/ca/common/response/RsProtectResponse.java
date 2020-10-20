package io.jans.ca.common.response;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsProtectResponse implements IOpResponse {

    @JsonProperty(value = "rp_id")
    private String rpId;

    public RsProtectResponse() {
    }

    public RsProtectResponse(String rpId) {
        this.rpId = rpId;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }
}

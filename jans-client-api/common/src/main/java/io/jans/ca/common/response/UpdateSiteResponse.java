package io.jans.ca.common.response;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2016
 */

public class UpdateSiteResponse implements IOpResponse {

    @JsonProperty(value = "rp_id")
    private String rpId;

    public UpdateSiteResponse() {
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }
}

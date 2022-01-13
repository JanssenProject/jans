package io.jans.ca.common.response;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yuriyz
 */
public class RemoveSiteResponse implements IOpResponse {

    @JsonProperty(value = "rp_id")
    private String rpId;

    public RemoveSiteResponse() {
    }

    public RemoveSiteResponse(String rpId) {
        this.rpId = rpId;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }
}

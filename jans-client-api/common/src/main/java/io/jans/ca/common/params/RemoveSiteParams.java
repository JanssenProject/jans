package io.jans.ca.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoveSiteParams implements HasRpIdParams {

    @JsonProperty(value = "rp_id")
    private String rp_id;

    public RemoveSiteParams() {
    }

    public RemoveSiteParams(String oxdId) {
        this(oxdId, null);
    }

    public RemoveSiteParams(String oxdId, String token) {
        this.rp_id = oxdId;
    }

    @Override
    public String getRpId() {
        return rp_id;
    }

    public void setRpId(String rpId) {
        this.rp_id = rpId;
    }

    @Override
    public String toString() {
        return "RemoveSiteParams{" +
                "rp_id='" + rp_id + '\'' +
                '}';
    }
}

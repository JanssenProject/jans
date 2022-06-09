package io.jans.ca.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRpParams implements HasRpIdParams {

    @JsonProperty(value = "rp_id")
    private String rp_id;
    @JsonProperty(value = "list")
    private Boolean list;

    public GetRpParams() {
    }

    public GetRpParams(String rpId) {
        this.rp_id = rpId;
    }

    public String getRpId() {
        return rp_id;
    }

    public void setRpId(String rpId) {
        this.rp_id = rpId;
    }

    public Boolean getList() {
        return list;
    }

    public void setList(Boolean list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "GetRpParams{" +
                "rpId='" + rp_id + '\'' +
                "list='" + list + '\'' +
                '}';
    }
}

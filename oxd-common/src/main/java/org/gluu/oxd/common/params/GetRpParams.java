package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRpParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "list")
    private Boolean list;

    public GetRpParams() {
    }

    public GetRpParams(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
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
                "oxdId='" + oxd_id + '\'' +
                "list='" + list + '\'' +
                '}';
    }
}

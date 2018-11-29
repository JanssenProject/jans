package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRpParams implements IParams {

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
                '}';
    }
}

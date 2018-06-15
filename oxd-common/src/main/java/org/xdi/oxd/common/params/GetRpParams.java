package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRpParams implements IParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "list")
    private Boolean list;

    public GetRpParams() {
    }

    public GetRpParams(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
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
                "oxdId='" + oxdId + '\'' +
                '}';
    }
}

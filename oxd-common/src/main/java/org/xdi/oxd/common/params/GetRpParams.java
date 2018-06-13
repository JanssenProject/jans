package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRpParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

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

    @Override
    public String toString() {
        return "GetRpParams{" +
                "oxdId='" + oxdId + '\'' +
                '}';
    }
}

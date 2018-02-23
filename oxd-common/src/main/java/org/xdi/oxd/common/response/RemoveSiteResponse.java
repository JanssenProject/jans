package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author yuriyz
 */
public class RemoveSiteResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    public RemoveSiteResponse() {
    }

    public RemoveSiteResponse(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }
}

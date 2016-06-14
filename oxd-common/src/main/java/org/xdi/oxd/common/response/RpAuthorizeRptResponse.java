package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/06/2016
 */

public class RpAuthorizeRptResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    public RpAuthorizeRptResponse() {
    }

    public RpAuthorizeRptResponse(String oxdId) {
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
        final StringBuilder sb = new StringBuilder();
        sb.append("RpAuthorizeRptResponse");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

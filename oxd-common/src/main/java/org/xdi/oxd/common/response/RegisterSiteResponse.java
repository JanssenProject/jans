package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 29/09/2015
 */

public class RegisterSiteResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    public RegisterSiteResponse() {
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
        sb.append("RegisterSiteResponse");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

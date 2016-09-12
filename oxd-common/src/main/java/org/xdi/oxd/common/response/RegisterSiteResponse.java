package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 29/09/2015
 */

public class RegisterSiteResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "op_host")
    private String opHost;

    public RegisterSiteResponse() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getOpHost() {
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterSiteResponse");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append("{opHost='").append(opHost).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

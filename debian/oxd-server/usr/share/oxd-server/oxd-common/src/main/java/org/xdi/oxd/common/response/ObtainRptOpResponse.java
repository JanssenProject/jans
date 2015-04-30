package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class ObtainRptOpResponse implements IOpResponse {

    @JsonProperty(value = "rpt_token")
    private String rptToken;

    public ObtainRptOpResponse() {
    }

    public String getRptToken() {
        return rptToken;
    }

    public void setRptToken(String p_rptToken) {
        rptToken = p_rptToken;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ObtainRptOpResponse");
        sb.append("{rptToken='").append(rptToken).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

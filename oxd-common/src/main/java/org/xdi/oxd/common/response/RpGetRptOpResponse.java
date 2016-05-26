/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class RpGetRptOpResponse implements IOpResponse {

    @JsonProperty(value = "rpt")
    private String rpt;

    public RpGetRptOpResponse() {
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String p_rptToken) {
        rpt = p_rptToken;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RpGetRptOpResponse");
        sb.append("{rpt='").append(rpt).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

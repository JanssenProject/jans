/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class RpGetRptResponse implements IOpResponse {

    @JsonProperty(value = "access_token")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "access_token")
    private String rpt;
    @JsonProperty(value = "token_type")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "token_type")
    private String tokenType;
    @JsonProperty(value = "pct")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "pct")
    private String pct;
    @JsonProperty(value = "updated")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "updated")
    private Boolean updated;

    public RpGetRptResponse() {
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String p_rptToken) {
        rpt = p_rptToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getPct() {
        return pct;
    }

    public void setPct(String pct) {
        this.pct = pct;
    }

    public Boolean getUpdated() {
        return updated;
    }

    public void setUpdated(Boolean updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        return "RpGetRptResponse{" +
                "rpt='" + rpt + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", pct='" + pct + '\'' +
                ", updated=" + updated +
                '}';
    }
}

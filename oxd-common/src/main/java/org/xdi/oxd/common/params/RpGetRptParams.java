/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpGetRptParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;

    public RpGetRptParams() {
    }

    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
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
        sb.append("RpGetRptParams");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append(", protectionAccessToken=").append(protectionAccessToken);
        sb.append('}');
        return sb.toString();
    }
}

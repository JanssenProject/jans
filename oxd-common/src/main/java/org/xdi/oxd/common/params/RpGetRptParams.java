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
public class RpGetRptParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "force_new")
    private boolean forceNew;

    public RpGetRptParams() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public boolean isForceNew() {
        return forceNew;
    }

    public void setForceNew(boolean forceNew) {
        this.forceNew = forceNew;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RpGetRptParams");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append(", forceNew=").append(forceNew);
        sb.append('}');
        return sb.toString();
    }
}

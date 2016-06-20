package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.xdi.oxd.rs.protect.RsResourceList;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsProtectParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonUnwrapped
    @JsonProperty(value = "resources")
    private RsResourceList resources;

    public RsProtectParams() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public RsResourceList getResources() {
        return resources;
    }

    public void setResources(RsResourceList resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RsProtectParams");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append(", resources=").append(resources);
        sb.append('}');
        return sb.toString();
    }
}

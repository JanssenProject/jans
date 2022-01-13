package io.jans.ca.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.ca.rs.protect.RsResource;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RsProtectParams implements HasRpIdParams {

    @JsonProperty(value = "rp_id")
    private String rp_id;
    @JsonProperty(value = "resources")
    private List<RsResource> resources;
    @JsonProperty(value = "overwrite")
    private Boolean overwrite = false;

    public RsProtectParams() {
    }

    public String getRpId() {
        return rp_id;
    }

    public void setRpId(String rpId) {
        this.rp_id = rpId;
    }

    public List<RsResource> getResources() {
        return resources;
    }

    public void setResources(List<RsResource> resources) {
        this.resources = resources;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RsProtectParams");
        sb.append("{rp_id='").append(rp_id).append('\'');
        sb.append(", resources=").append(resources);
        sb.append(", overwrite=").append(overwrite);
        sb.append('}');
        return sb.toString();
    }
}

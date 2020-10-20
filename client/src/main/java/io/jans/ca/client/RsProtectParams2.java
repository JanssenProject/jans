package io.jans.ca.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.ca.common.params.HasRpIdParams;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RsProtectParams2 implements HasRpIdParams {

    @JsonProperty(value = "rp_id")
    private String rp_id;
    @JsonProperty(value = "resources")
    private JsonNode resources;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;
    @JsonProperty(value = "overwrite")
    private Boolean overwrite = false;

    public RsProtectParams2() {
    }

    public String getToken() {
        return protection_access_token;
    }

    public void setToken(String token) {
        this.protection_access_token = token;
    }

    public String getRpId() {
        return rp_id;
    }

    public void setRpId(String rpId) {
        this.rp_id = rpId;
    }

    public JsonNode getResources() {
        return resources;
    }

    public void setResources(JsonNode resources) {
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
        sb.append(", protection_access_token=").append(protection_access_token);
        sb.append(", overwrite=").append(overwrite);
        sb.append('}');
        return sb.toString();
    }
}

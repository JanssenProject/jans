package org.gluu.oxd.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.gluu.oxd.common.params.HasProtectionAccessTokenParams;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RsProtectParams2 implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "resources")
    private JsonNode resources;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;
    @JsonProperty(value = "overwrite")
    private Boolean overwrite = false;

    public RsProtectParams2() {
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
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
        sb.append("{oxd_id='").append(oxd_id).append('\'');
        sb.append(", resources=").append(resources);
        sb.append(", protection_access_token=").append(protection_access_token);
        sb.append(", overwrite=").append(overwrite);
        sb.append('}');
        return sb.toString();
    }
}

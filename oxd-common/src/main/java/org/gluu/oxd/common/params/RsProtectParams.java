package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.gluu.oxd.rs.protect.RsResource;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RsProtectParams implements HasAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "resources")
    private List<RsResource> resources;
    @JsonProperty(value = "token")
    private String token;
    @JsonProperty(value = "overwrite")
    private Boolean overwrite = false;

    public RsProtectParams() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
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
        sb.append("{oxd_id='").append(oxd_id).append('\'');
        sb.append(", resources=").append(resources);
        sb.append(", token=").append(token);
        sb.append(", overwrite=").append(overwrite);
        sb.append('}');
        return sb.toString();
    }
}

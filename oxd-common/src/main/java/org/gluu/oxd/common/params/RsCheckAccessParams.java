package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/06/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RsCheckAccessParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "rpt")
    private String rpt;
    @JsonProperty(value = "path")
    private String path;
    @JsonProperty(value = "http_method")
    private String http_method;
    @JsonProperty(value = "scopes")
    private List<String> scopes;

    public RsCheckAccessParams() {
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return http_method;
    }

    public void setHttpMethod(String httpMethod) {
        this.http_method = httpMethod;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RsCheckAccessParams");
        sb.append("{oxd_id='").append(oxd_id).append('\'');
        sb.append(", rpt='").append(rpt).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append(", http_method='").append(http_method).append('\'');
        sb.append(", scopes='").append(scopes).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

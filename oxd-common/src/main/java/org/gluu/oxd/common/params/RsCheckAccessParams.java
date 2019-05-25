package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/06/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RsCheckAccessParams implements HasAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "rpt")
    private String rpt;
    @JsonProperty(value = "path")
    private String path;
    @JsonProperty(value = "http_method")
    private String http_method;
    @JsonProperty(value = "token")
    private String token;

    public RsCheckAccessParams() {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RsCheckAccessParams");
        sb.append("{oxd_id='").append(oxd_id).append('\'');
        sb.append(", rpt='").append(rpt).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append(", http_method='").append(http_method).append('\'');
        sb.append(", token='").append(token).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

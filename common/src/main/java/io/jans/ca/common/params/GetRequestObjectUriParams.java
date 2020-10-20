package io.jans.ca.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRequestObjectUriParams implements HasRpIdParams {

    @JsonProperty(value = "rp_id")
    private String rp_id;
    @JsonProperty(value = "params")
    private Map<String, Object> params;
    @JsonProperty(value = "request_object_signing_alg")
    private String request_object_signing_alg;
    @JsonProperty(value = "oxd_host_url")
    private String oxd_host_url;

    public String getRpId() {
        return rp_id;
    }

    public void setRpId(String rpId) {
        this.rp_id = rpId;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getRequestObjectSigningAlg() {
        return request_object_signing_alg;
    }

    public void setRequestObjectSigningAlg(String request_object_signing_alg) {
        this.request_object_signing_alg = request_object_signing_alg;
    }

    public String getRpHostUrl() {
        return oxd_host_url;
    }

    public void setRpHostUrl(String oxd_host_url) {
        this.oxd_host_url = oxd_host_url;
    }
    @Override
    public String toString() {
        return "GetRequestUri{" +
                "rp_id='" + rp_id + '\'' +
                ", params=" + params +
                ", request_object_signing_alg=" + request_object_signing_alg +
                ", oxd_host_url=" + oxd_host_url +
                '}';
    }
}

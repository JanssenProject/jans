package io.jans.fido2.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
abstract public class SuperGluuSupport {

    @JsonProperty(value = "super_gluu_app_id")
    private String superGluuAppId;
    @JsonProperty(value = "super_gluu_request_mode")
    private String SuperGluuRequestMode;
    @JsonProperty(value = "super_gluu_request")
    private Boolean SuperGluuRequest;
    @JsonProperty(value = "super_gluu_key_handle")
    private String SuperGluuKeyHandle;
    @JsonProperty(value = "super_gluu_request_cancel")
    private Boolean SuperGluuRequestCancel;

    public String getSuperGluuAppId() {
        return superGluuAppId;
    }

    public void setSuperGluuAppId(String superGluuAppId) {
        this.superGluuAppId = superGluuAppId;
    }

    public String getSuperGluuRequestMode() {
        return SuperGluuRequestMode;
    }

    public void setSuperGluuRequestMode(String superGluuRequestMode) {
        SuperGluuRequestMode = superGluuRequestMode;
    }

    public Boolean getSuperGluuRequest() {
        return SuperGluuRequest;
    }

    public void setSuperGluuRequest(Boolean superGluuRequest) {
        SuperGluuRequest = superGluuRequest;
    }

    public String getSuperGluuKeyHandle() {
        return SuperGluuKeyHandle;
    }

    public void setSuperGluuKeyHandle(String superGluuKeyHandle) {
        SuperGluuKeyHandle = superGluuKeyHandle;
    }

    public Boolean getSuperGluuRequestCancel() {
        return SuperGluuRequestCancel;
    }

    public void setSuperGluuRequestCancel(Boolean superGluuRequestCancel) {
        SuperGluuRequestCancel = superGluuRequestCancel;
    }
}

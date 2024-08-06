package io.jans.fido2.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
abstract public class SuperGluuSupport {

    @JsonProperty(value = "super_gluu_app_id")
    private String superGluuAppId;
    @JsonProperty(value = "super_gluu_request_mode")
    private String superGluuRequestMode;
    @JsonProperty(value = "super_gluu_request")
    private Boolean superGluuRequest;
    @JsonProperty(value = "super_gluu_key_handle")
    private String superGluuKeyHandle;
    @JsonProperty(value = "super_gluu_request_cancel")
    private Boolean superGluuRequestCancel;

    public String getSuperGluuAppId() {
        return superGluuAppId;
    }

    public void setSuperGluuAppId(String superGluuAppId) {
        this.superGluuAppId = superGluuAppId;
    }

    public String getSuperGluuRequestMode() {
        return superGluuRequestMode;
    }

    public void setSuperGluuRequestMode(String superGluuRequestMode) {
        this.superGluuRequestMode = superGluuRequestMode;
    }

    public Boolean getSuperGluuRequest() {
        return superGluuRequest;
    }

    public void setSuperGluuRequest(Boolean superGluuRequest) {
        this.superGluuRequest = superGluuRequest;
    }

    public String getSuperGluuKeyHandle() {
        return superGluuKeyHandle;
    }

    public void setSuperGluuKeyHandle(String superGluuKeyHandle) {
        this.superGluuKeyHandle = superGluuKeyHandle;
    }

    public Boolean getSuperGluuRequestCancel() {
        return superGluuRequestCancel;
    }

    public void setSuperGluuRequestCancel(Boolean superGluuRequestCancel) {
        this.superGluuRequestCancel = superGluuRequestCancel;
    }
}

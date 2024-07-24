package io.jans.fido2.model.common;

abstract public class SuperGluuSupport {
    private String super_gluu_app_id;
    private String super_gluu_request_mode;
    private Boolean super_gluu_request;
    private String super_gluu_key_handle;
    private Boolean super_gluu_request_cancel;

    public String getSuper_gluu_app_id() {
        return super_gluu_app_id;
    }

    public void setSuper_gluu_app_id(String super_gluu_app_id) {
        this.super_gluu_app_id = super_gluu_app_id;
    }

    public String getSuper_gluu_request_mode() {
        return super_gluu_request_mode;
    }

    public void setSuper_gluu_request_mode(String super_gluu_request_mode) {
        this.super_gluu_request_mode = super_gluu_request_mode;
    }

    public Boolean getSuper_gluu_request() {
        return super_gluu_request;
    }

    public void setSuper_gluu_request(Boolean super_gluu_request) {
        this.super_gluu_request = super_gluu_request;
    }

    public String getSuper_gluu_key_handle() {
        return super_gluu_key_handle;
    }

    public void setSuper_gluu_key_handle(String super_gluu_key_handle) {
        this.super_gluu_key_handle = super_gluu_key_handle;
    }

    public Boolean getSuper_gluu_request_cancel() {
        return super_gluu_request_cancel;
    }

    public void setSuper_gluu_request_cancel(Boolean super_gluu_request_cancel) {
        this.super_gluu_request_cancel = super_gluu_request_cancel;
    }
}

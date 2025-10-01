package io.jans.as.model.config.adminui;

public class UIConfiguration {
    private Integer sessionTimeoutInMins;
    private Boolean allowSmtpKeystoreEdit;
    private String cedarlingLogType;

    public UIConfiguration() {
        //Do not remove
    }

    public Integer getSessionTimeoutInMins() {
        return sessionTimeoutInMins;
    }

    public void setSessionTimeoutInMins(Integer sessionTimeoutInMins) {
        this.sessionTimeoutInMins = sessionTimeoutInMins;
    }

    public Boolean getAllowSmtpKeystoreEdit() {
        return allowSmtpKeystoreEdit;
    }

    public void setAllowSmtpKeystoreEdit(Boolean allowSmtpKeystoreEdit) {
        this.allowSmtpKeystoreEdit = allowSmtpKeystoreEdit;
    }

    public String getCedarlingLogType() {
        return cedarlingLogType;
    }

    public void setCedarlingLogType(String cedarlingLogType) {
        this.cedarlingLogType = cedarlingLogType;
    }
}

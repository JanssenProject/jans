package io.jans.as.model.config.adminui;

public class UIConfiguration {
    private Integer sessionTimeoutInMins;
    private Boolean allowSmtpKeystoreEdit;
    private String cedarlingLogType;
    private String auiPolicyStoreUrl;
    private String auiDefaultPolicyStorePath;
    private Boolean useRemotePolicyStore ;

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

    public String getAuiPolicyStoreUrl() {
        return auiPolicyStoreUrl;
    }

    public void setAuiPolicyStoreUrl(String auiPolicyStoreUrl) {
        this.auiPolicyStoreUrl = auiPolicyStoreUrl;
    }

    public Boolean getUseRemotePolicyStore() {
        return useRemotePolicyStore;
    }

    public void setUseRemotePolicyStore(Boolean useRemotePolicyStore) {
        this.useRemotePolicyStore = useRemotePolicyStore;
    }

    public String getAuiDefaultPolicyStorePath() {
        return auiDefaultPolicyStorePath;
    }

    public void setAuiDefaultPolicyStorePath(String auiDefaultPolicyStorePath) {
        this.auiDefaultPolicyStorePath = auiDefaultPolicyStorePath;
    }
}

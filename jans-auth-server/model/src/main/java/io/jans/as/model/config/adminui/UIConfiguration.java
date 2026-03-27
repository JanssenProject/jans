package io.jans.as.model.config.adminui;

public class UIConfiguration {
    private Integer sessionTimeoutInMins;
    private Boolean allowSmtpKeystoreEdit;
    private String cedarlingLogType;
    private String auiPolicyStoreUrl;
    private String auiDefaultPolicyStorePath;

    /**
     * Default no-argument constructor kept for frameworks and serialization.
     *
     * Intentionally empty; do not remove.
     */
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

    /**
     * Sets the base URL the admin UI will use to access the AUI policy store.
     *
     * @param auiPolicyStoreUrl the policy store base URL, or {@code null} to unset it
     */
    public void setAuiPolicyStoreUrl(String auiPolicyStoreUrl) {
        this.auiPolicyStoreUrl = auiPolicyStoreUrl;
    }

    /**
     * Gets the default filesystem path used by the admin UI for the policy store.
     *
     * @return the default policy store path, or null if not configured
     */
    public String getAuiDefaultPolicyStorePath() {
        return auiDefaultPolicyStorePath;
    }

    public void setAuiDefaultPolicyStorePath(String auiDefaultPolicyStorePath) {
        this.auiDefaultPolicyStorePath = auiDefaultPolicyStorePath;
    }
}

package io.jans.ca.plugin.adminui.model.config;


import io.jans.as.model.config.adminui.KeyValuePair;
import io.jans.ca.plugin.adminui.model.adminui.CedarlingLogType;

import java.util.List;

public class AUIConfiguration {

    private String appType;
    //Admin UI Web
    private String auiWebServerHost;
    private String auiWebServerClientId;
    private String auiWebServerClientSecret;
    private String auiWebServerScope;
    private String auiWebServerAcrValues;
    private String auiWebServerRedirectUrl;
    private String auiWebServerFrontChannelLogoutUrl;
    private String auiWebServerPostLogoutRedirectUri;
    private String auiWebServerAuthzBaseUrl;
    private String auiWebServerTokenEndpoint;
    private String auiWebServerIntrospectionEndpoint;
    private String auiWebServerUserInfoEndpoint;
    private String auiWebServerEndSessionEndpoint;
    //Admin UI Backend API
    private String auiBackendApiServerClientId;
    private String auiBackendApiServerClientSecret;
    private String auiBackendApiServerScope;
    private String auiBackendApiServerAcrValues;
    private String auiBackendApiServerRedirectUrl;
    private String auiBackendApiServerFrontChannelLogoutUrl;
    private String auiBackendApiServerPostLogoutRedirectUri;
    private String auiBackendApiServerAuthzBaseUrl;
    private String auiBackendApiServerTokenEndpoint;
    private String auiBackendApiServerIntrospectionEndpoint;
    private String auiBackendApiServerUserInfoEndpoint;
    private String auiBackendApiServerEndSessionEndpoint;
    // LicenseSpring
    private LicenseConfiguration licenseConfiguration;
    //UI session timeout
    private Integer sessionTimeoutInMins;
    private Boolean allowSmtpKeystoreEdit;
    private List<KeyValuePair> additionalParameters;
    private CedarlingLogType cedarlingLogType;
    private String auiCedarlingPolicyStoreUrl;
    private String auiCedarlingDefaultPolicyStorePath;
    private Boolean useCedarlingRemotePolicyStore = Boolean.FALSE;

    public List<KeyValuePair> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(List<KeyValuePair> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    public Integer getSessionTimeoutInMins() {
        return sessionTimeoutInMins;
    }

    public void setSessionTimeoutInMins(Integer sessionTimeoutInMins) {
        this.sessionTimeoutInMins = sessionTimeoutInMins;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getAuiWebServerHost() {
        return auiWebServerHost;
    }

    public void setAuiWebServerHost(String auiWebServerHost) {
        this.auiWebServerHost = auiWebServerHost;
    }

    public String getAuiWebServerClientId() {
        return auiWebServerClientId;
    }

    public void setAuiWebServerClientId(String auiWebServerClientId) {
        this.auiWebServerClientId = auiWebServerClientId;
    }

    public String getAuiWebServerClientSecret() {
        return auiWebServerClientSecret;
    }

    public void setAuiWebServerClientSecret(String auiWebServerClientSecret) {
        this.auiWebServerClientSecret = auiWebServerClientSecret;
    }

    public String getAuiWebServerScope() {
        return auiWebServerScope;
    }

    public void setAuiWebServerScope(String auiWebServerScope) {
        this.auiWebServerScope = auiWebServerScope;
    }

    public String getAuiWebServerAcrValues() {
        return auiWebServerAcrValues;
    }

    public void setAuiWebServerAcrValues(String auiWebServerAcrValues) {
        this.auiWebServerAcrValues = auiWebServerAcrValues;
    }

    public String getAuiWebServerRedirectUrl() {
        return auiWebServerRedirectUrl;
    }

    public void setAuiWebServerRedirectUrl(String auiWebServerRedirectUrl) {
        this.auiWebServerRedirectUrl = auiWebServerRedirectUrl;
    }

    public String getAuiWebServerFrontChannelLogoutUrl() {
        return auiWebServerFrontChannelLogoutUrl;
    }

    public void setAuiWebServerFrontChannelLogoutUrl(String auiWebServerFrontChannelLogoutUrl) {
        this.auiWebServerFrontChannelLogoutUrl = auiWebServerFrontChannelLogoutUrl;
    }

    public String getAuiWebServerPostLogoutRedirectUri() {
        return auiWebServerPostLogoutRedirectUri;
    }

    public void setAuiWebServerPostLogoutRedirectUri(String auiWebServerPostLogoutRedirectUri) {
        this.auiWebServerPostLogoutRedirectUri = auiWebServerPostLogoutRedirectUri;
    }

    public String getAuiWebServerAuthzBaseUrl() {
        return auiWebServerAuthzBaseUrl;
    }

    public void setAuiWebServerAuthzBaseUrl(String auiWebServerAuthzBaseUrl) {
        this.auiWebServerAuthzBaseUrl = auiWebServerAuthzBaseUrl;
    }

    public String getAuiWebServerTokenEndpoint() {
        return auiWebServerTokenEndpoint;
    }

    public void setAuiWebServerTokenEndpoint(String auiWebServerTokenEndpoint) {
        this.auiWebServerTokenEndpoint = auiWebServerTokenEndpoint;
    }

    public String getAuiWebServerIntrospectionEndpoint() {
        return auiWebServerIntrospectionEndpoint;
    }

    public void setAuiWebServerIntrospectionEndpoint(String auiWebServerIntrospectionEndpoint) {
        this.auiWebServerIntrospectionEndpoint = auiWebServerIntrospectionEndpoint;
    }

    public String getAuiWebServerUserInfoEndpoint() {
        return auiWebServerUserInfoEndpoint;
    }

    public void setAuiWebServerUserInfoEndpoint(String auiWebServerUserInfoEndpoint) {
        this.auiWebServerUserInfoEndpoint = auiWebServerUserInfoEndpoint;
    }

    public String getAuiWebServerEndSessionEndpoint() {
        return auiWebServerEndSessionEndpoint;
    }

    public void setAuiWebServerEndSessionEndpoint(String auiWebServerEndSessionEndpoint) {
        this.auiWebServerEndSessionEndpoint = auiWebServerEndSessionEndpoint;
    }

    public String getAuiBackendApiServerClientId() {
        return auiBackendApiServerClientId;
    }

    public void setAuiBackendApiServerClientId(String auiBackendApiServerClientId) {
        this.auiBackendApiServerClientId = auiBackendApiServerClientId;
    }

    public String getAuiBackendApiServerClientSecret() {
        return auiBackendApiServerClientSecret;
    }

    public void setAuiBackendApiServerClientSecret(String auiBackendApiServerClientSecret) {
        this.auiBackendApiServerClientSecret = auiBackendApiServerClientSecret;
    }

    public String getAuiBackendApiServerScope() {
        return auiBackendApiServerScope;
    }

    public void setAuiBackendApiServerScope(String auiBackendApiServerScope) {
        this.auiBackendApiServerScope = auiBackendApiServerScope;
    }

    public String getAuiBackendApiServerAcrValues() {
        return auiBackendApiServerAcrValues;
    }

    public void setAuiBackendApiServerAcrValues(String auiBackendApiServerAcrValues) {
        this.auiBackendApiServerAcrValues = auiBackendApiServerAcrValues;
    }

    public String getAuiBackendApiServerRedirectUrl() {
        return auiBackendApiServerRedirectUrl;
    }

    public void setAuiBackendApiServerRedirectUrl(String auiBackendApiServerRedirectUrl) {
        this.auiBackendApiServerRedirectUrl = auiBackendApiServerRedirectUrl;
    }

    public String getAuiBackendApiServerFrontChannelLogoutUrl() {
        return auiBackendApiServerFrontChannelLogoutUrl;
    }

    public void setAuiBackendApiServerFrontChannelLogoutUrl(String auiBackendApiServerFrontChannelLogoutUrl) {
        this.auiBackendApiServerFrontChannelLogoutUrl = auiBackendApiServerFrontChannelLogoutUrl;
    }

    public String getAuiBackendApiServerPostLogoutRedirectUri() {
        return auiBackendApiServerPostLogoutRedirectUri;
    }

    public void setAuiBackendApiServerPostLogoutRedirectUri(String auiBackendApiServerPostLogoutRedirectUri) {
        this.auiBackendApiServerPostLogoutRedirectUri = auiBackendApiServerPostLogoutRedirectUri;
    }

    public String getAuiBackendApiServerAuthzBaseUrl() {
        return auiBackendApiServerAuthzBaseUrl;
    }

    public void setAuiBackendApiServerAuthzBaseUrl(String auiBackendApiServerAuthzBaseUrl) {
        this.auiBackendApiServerAuthzBaseUrl = auiBackendApiServerAuthzBaseUrl;
    }

    public String getAuiBackendApiServerTokenEndpoint() {
        return auiBackendApiServerTokenEndpoint;
    }

    public void setAuiBackendApiServerTokenEndpoint(String auiBackendApiServerTokenEndpoint) {
        this.auiBackendApiServerTokenEndpoint = auiBackendApiServerTokenEndpoint;
    }

    public String getAuiBackendApiServerIntrospectionEndpoint() {
        return auiBackendApiServerIntrospectionEndpoint;
    }

    public void setAuiBackendApiServerIntrospectionEndpoint(String auiBackendApiServerIntrospectionEndpoint) {
        this.auiBackendApiServerIntrospectionEndpoint = auiBackendApiServerIntrospectionEndpoint;
    }

    public String getAuiBackendApiServerUserInfoEndpoint() {
        return auiBackendApiServerUserInfoEndpoint;
    }

    public void setAuiBackendApiServerUserInfoEndpoint(String auiBackendApiServerUserInfoEndpoint) {
        this.auiBackendApiServerUserInfoEndpoint = auiBackendApiServerUserInfoEndpoint;
    }

    public String getAuiBackendApiServerEndSessionEndpoint() {
        return auiBackendApiServerEndSessionEndpoint;
    }

    public void setAuiBackendApiServerEndSessionEndpoint(String auiBackendApiServerEndSessionEndpoint) {
        this.auiBackendApiServerEndSessionEndpoint = auiBackendApiServerEndSessionEndpoint;
    }

    public LicenseConfiguration getLicenseConfiguration() {
        return licenseConfiguration;
    }

    public void setLicenseConfiguration(LicenseConfiguration licenseConfiguration) {
        this.licenseConfiguration = licenseConfiguration;
    }

    public Boolean getAllowSmtpKeystoreEdit() {
        return allowSmtpKeystoreEdit;
    }

    public void setAllowSmtpKeystoreEdit(Boolean allowSmtpKeystoreEdit) {
        this.allowSmtpKeystoreEdit = allowSmtpKeystoreEdit;
    }

    public CedarlingLogType getCedarlingLogType() {
        return cedarlingLogType;
    }

    public void setCedarlingLogType(CedarlingLogType cedarlingLogType) {
        this.cedarlingLogType = cedarlingLogType;
    }

    public String getAuiCedarlingPolicyStoreUrl() {
        return auiCedarlingPolicyStoreUrl;
    }

    public void setAuiCedarlingPolicyStoreUrl(String auiCedarlingPolicyStoreUrl) {
        this.auiCedarlingPolicyStoreUrl = auiCedarlingPolicyStoreUrl;
    }

    public String getAuiCedarlingDefaultPolicyStorePath() {
        return auiCedarlingDefaultPolicyStorePath;
    }

    public void setAuiCedarlingDefaultPolicyStorePath(String auiCedarlingDefaultPolicyStorePath) {
        this.auiCedarlingDefaultPolicyStorePath = auiCedarlingDefaultPolicyStorePath;
    }

    public Boolean getUseCedarlingRemotePolicyStore() {
        return useCedarlingRemotePolicyStore;
    }

    public void setUseCedarlingRemotePolicyStore(Boolean useCedarlingRemotePolicyStore) {
        this.useCedarlingRemotePolicyStore = useCedarlingRemotePolicyStore;
    }

    @Override
    public String toString() {
        return "AUIConfiguration{" +
                "appType='" + appType + '\'' +
                ", auiWebServerHost='" + auiWebServerHost + '\'' +
                ", auiWebServerClientId='" + auiWebServerClientId + '\'' +
                ", auiWebServerScope='" + auiWebServerScope + '\'' +
                ", auiWebServerAcrValues='" + auiWebServerAcrValues + '\'' +
                ", auiWebServerRedirectUrl='" + auiWebServerRedirectUrl + '\'' +
                ", auiWebServerFrontChannelLogoutUrl='" + auiWebServerFrontChannelLogoutUrl + '\'' +
                ", auiWebServerPostLogoutRedirectUri='" + auiWebServerPostLogoutRedirectUri + '\'' +
                ", auiWebServerAuthzBaseUrl='" + auiWebServerAuthzBaseUrl + '\'' +
                ", auiWebServerTokenEndpoint='" + auiWebServerTokenEndpoint + '\'' +
                ", auiWebServerIntrospectionEndpoint='" + auiWebServerIntrospectionEndpoint + '\'' +
                ", auiWebServerUserInfoEndpoint='" + auiWebServerUserInfoEndpoint + '\'' +
                ", auiWebServerEndSessionEndpoint='" + auiWebServerEndSessionEndpoint + '\'' +
                ", auiBackendApiServerClientId='" + auiBackendApiServerClientId + '\'' +
                ", auiBackendApiServerScope='" + auiBackendApiServerScope + '\'' +
                ", auiBackendApiServerAcrValues='" + auiBackendApiServerAcrValues + '\'' +
                ", auiBackendApiServerRedirectUrl='" + auiBackendApiServerRedirectUrl + '\'' +
                ", auiBackendApiServerFrontChannelLogoutUrl='" + auiBackendApiServerFrontChannelLogoutUrl + '\'' +
                ", auiBackendApiServerPostLogoutRedirectUri='" + auiBackendApiServerPostLogoutRedirectUri + '\'' +
                ", auiBackendApiServerAuthzBaseUrl='" + auiBackendApiServerAuthzBaseUrl + '\'' +
                ", auiBackendApiServerTokenEndpoint='" + auiBackendApiServerTokenEndpoint + '\'' +
                ", auiBackendApiServerIntrospectionEndpoint='" + auiBackendApiServerIntrospectionEndpoint + '\'' +
                ", auiBackendApiServerUserInfoEndpoint='" + auiBackendApiServerUserInfoEndpoint + '\'' +
                ", auiBackendApiServerEndSessionEndpoint='" + auiBackendApiServerEndSessionEndpoint + '\'' +
                ", licenseConfiguration=" + licenseConfiguration +
                ", sessionTimeoutInMins=" + sessionTimeoutInMins +
                ", allowSmtpKeystoreEdit=" + allowSmtpKeystoreEdit +
                ", additionalParameters=" + additionalParameters +
                ", cedarlingLogType=" + cedarlingLogType +
                ", auiCedarlingPolicyStoreUrl='" + auiCedarlingPolicyStoreUrl + '\'' +
                ", auiCedarlingDefaultPolicyStorePath='" + auiCedarlingDefaultPolicyStorePath + '\'' +
                ", useCedarlingRemotePolicyStore=" + useCedarlingRemotePolicyStore +
                '}';
    }
}

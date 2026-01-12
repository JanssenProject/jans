package io.jans.configapi.core.model.adminui;

import io.jans.as.model.config.adminui.KeyValuePair;

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
    private CedarlingPolicyStrRetrievalPoint cedarlingPolicyStoreRetrievalPoint = CedarlingPolicyStrRetrievalPoint.DEFAULT;

    /**
     * Retrieves the additional key-value parameters configured for the Admin UI.
     *
     * @return the list of additional parameters, or null if none are set
     */
    public List<KeyValuePair> getAdditionalParameters() {
        return additionalParameters;
    }

    /**
     * Set additional key-value parameters for the configuration.
     *
     * @param additionalParameters the list of extra KeyValuePair entries, or {@code null} to clear them
     */
    public void setAdditionalParameters(List<KeyValuePair> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    /**
     * Get the UI session timeout value in minutes.
     *
     * @return the session timeout in minutes, or `null` if not set
     */
    public Integer getSessionTimeoutInMins() {
        return sessionTimeoutInMins;
    }

    /**
     * Sets the UI session timeout in minutes.
     *
     * @param sessionTimeoutInMins the session timeout in minutes, or {@code null} to unset
     */
    public void setSessionTimeoutInMins(Integer sessionTimeoutInMins) {
        this.sessionTimeoutInMins = sessionTimeoutInMins;
    }

    /**
     * Gets the application type identifier for the Admin UI.
     *
     * @return the application type identifier, or null if not set
     */
    public String getAppType() {
        return appType;
    }

    /**
     * Sets the application type.
     *
     * @param appType the application type identifier for this configuration
     */
    public void setAppType(String appType) {
        this.appType = appType;
    }

    /**
     * Gets the host configured for the Admin UI web server.
     *
     * @return the Admin UI web server host, or `null` if not set
     */
    public String getAuiWebServerHost() {
        return auiWebServerHost;
    }

    /**
     * Sets the Admin UI web server host.
     *
     * @param auiWebServerHost the host (hostname or IP) for the Admin UI web server
     */
    public void setAuiWebServerHost(String auiWebServerHost) {
        this.auiWebServerHost = auiWebServerHost;
    }

    /**
     * Gets the client ID used by the Admin UI web server.
     *
     * @return the client ID for the Admin UI web server, or {@code null} if not set
     */
    public String getAuiWebServerClientId() {
        return auiWebServerClientId;
    }

    /**
     * Sets the client identifier used by the Admin UI web server.
     *
     * @param auiWebServerClientId the client ID for the Admin UI web server
     */
    public void setAuiWebServerClientId(String auiWebServerClientId) {
        this.auiWebServerClientId = auiWebServerClientId;
    }

    /**
     * Gets the Admin UI web server OAuth client secret.
     *
     * @return the client secret for the Admin UI web server, or {@code null} if not set.
     */
    public String getAuiWebServerClientSecret() {
        return auiWebServerClientSecret;
    }

    /**
     * Set the client secret used by the Admin UI web server's OAuth2 client.
     *
     * @param auiWebServerClientSecret the client secret string, or {@code null} to clear it
     */
    public void setAuiWebServerClientSecret(String auiWebServerClientSecret) {
        this.auiWebServerClientSecret = auiWebServerClientSecret;
    }

    /**
     * Gets the OAuth2/OpenID Connect scope used by the Admin UI web server.
     *
     * @return the scope string configured for the Admin UI web server, or {@code null} if not set
     */
    public String getAuiWebServerScope() {
        return auiWebServerScope;
    }

    /**
     * Sets the OAuth2/OpenID Connect scope used by the Admin UI web server.
     *
     * @param auiWebServerScope a space-separated scope string for the Admin UI web server's client
     */
    public void setAuiWebServerScope(String auiWebServerScope) {
        this.auiWebServerScope = auiWebServerScope;
    }

    /**
     * Retrieves the ACR values configured for the Admin UI web server.
     *
     * @return the ACR values string for the Admin UI web server, or {@code null} if not set
     */
    public String getAuiWebServerAcrValues() {
        return auiWebServerAcrValues;
    }

    /**
     * Sets the ACR (Authentication Context Class Reference) values used by the Admin UI web server.
     *
     * @param auiWebServerAcrValues space-separated ACR values to include in authentication requests
     */
    public void setAuiWebServerAcrValues(String auiWebServerAcrValues) {
        this.auiWebServerAcrValues = auiWebServerAcrValues;
    }

    /**
     * Gets the Admin UI web server's configured redirect URL.
     *
     * @return the configured redirect URL for the Admin UI web server, or null if not set
     */
    public String getAuiWebServerRedirectUrl() {
        return auiWebServerRedirectUrl;
    }

    /**
     * Sets the redirect URL used by the Admin UI web server.
     *
     * @param auiWebServerRedirectUrl the redirect URL to use for the Admin UI web server; may be null to clear the value
     */
    public void setAuiWebServerRedirectUrl(String auiWebServerRedirectUrl) {
        this.auiWebServerRedirectUrl = auiWebServerRedirectUrl;
    }

    /**
     * Gets the Admin UI web server front-channel logout URL.
     *
     * @return the front-channel logout URL for the Admin UI web server, or {@code null} if not configured
     */
    public String getAuiWebServerFrontChannelLogoutUrl() {
        return auiWebServerFrontChannelLogoutUrl;
    }

    /**
     * Sets the Admin UI Web Server front-channel logout URL.
     *
     * @param auiWebServerFrontChannelLogoutUrl the front-channel logout URL to use for the Admin UI web server
     */
    public void setAuiWebServerFrontChannelLogoutUrl(String auiWebServerFrontChannelLogoutUrl) {
        this.auiWebServerFrontChannelLogoutUrl = auiWebServerFrontChannelLogoutUrl;
    }

    /**
     * Gets the post-logout redirect URI configured for the Admin UI web server.
     *
     * @return the post-logout redirect URI, or {@code null} if not set
     */
    public String getAuiWebServerPostLogoutRedirectUri() {
        return auiWebServerPostLogoutRedirectUri;
    }

    /**
     * Sets the Admin UI web server's post-logout redirect URI.
     *
     * @param auiWebServerPostLogoutRedirectUri the URI to which the Admin UI web server will redirect after logout
     */
    public void setAuiWebServerPostLogoutRedirectUri(String auiWebServerPostLogoutRedirectUri) {
        this.auiWebServerPostLogoutRedirectUri = auiWebServerPostLogoutRedirectUri;
    }

    /**
     * Authorization server base URL used by the Admin UI web server to initiate authorization requests.
     *
     * @return the Admin UI web server's authorization base URL, or {@code null} if not set
     */
    public String getAuiWebServerAuthzBaseUrl() {
        return auiWebServerAuthzBaseUrl;
    }

    /**
     * Set the Admin UI web server authorization base URL.
     *
     * @param auiWebServerAuthzBaseUrl the authorization server base URL used by the Admin UI web server
     */
    public void setAuiWebServerAuthzBaseUrl(String auiWebServerAuthzBaseUrl) {
        this.auiWebServerAuthzBaseUrl = auiWebServerAuthzBaseUrl;
    }

    /**
     * Gets the token endpoint URL used by the Admin UI web server.
     *
     * @return the token endpoint URL for the Admin UI web server, or {@code null} if not set
     */
    public String getAuiWebServerTokenEndpoint() {
        return auiWebServerTokenEndpoint;
    }

    /**
     * Set the Admin UI web server token endpoint URL.
     *
     * @param auiWebServerTokenEndpoint the token endpoint URL for the Admin UI web server; may be null to unset
     */
    public void setAuiWebServerTokenEndpoint(String auiWebServerTokenEndpoint) {
        this.auiWebServerTokenEndpoint = auiWebServerTokenEndpoint;
    }

    /**
     * Gets the introspection endpoint URL for the Admin UI web server.
     *
     * @return the introspection endpoint URL for the Admin UI web server, or {@code null} if not set
     */
    public String getAuiWebServerIntrospectionEndpoint() {
        return auiWebServerIntrospectionEndpoint;
    }

    /**
     * Set the Admin UI web server introspection endpoint URL.
     *
     * @param auiWebServerIntrospectionEndpoint the introspection endpoint URL for the Admin UI web server
     */
    public void setAuiWebServerIntrospectionEndpoint(String auiWebServerIntrospectionEndpoint) {
        this.auiWebServerIntrospectionEndpoint = auiWebServerIntrospectionEndpoint;
    }

    /**
     * Retrieves the Admin UI web server UserInfo endpoint URL.
     *
     * @return the UserInfo endpoint URL for the Admin UI web server, or {@code null} if not set.
     */
    public String getAuiWebServerUserInfoEndpoint() {
        return auiWebServerUserInfoEndpoint;
    }

    /**
     * Set the Admin UI Web server UserInfo endpoint URL.
     *
     * @param auiWebServerUserInfoEndpoint the URL of the UserInfo endpoint used by the Admin UI Web server
     */
    public void setAuiWebServerUserInfoEndpoint(String auiWebServerUserInfoEndpoint) {
        this.auiWebServerUserInfoEndpoint = auiWebServerUserInfoEndpoint;
    }

    /**
     * End-session endpoint URL for the Admin UI web server.
     *
     * @return the end-session endpoint URL used by the Admin UI web server, or null if not set
     */
    public String getAuiWebServerEndSessionEndpoint() {
        return auiWebServerEndSessionEndpoint;
    }

    /**
     * Sets the Admin UI web server end-session endpoint URL.
     *
     * @param auiWebServerEndSessionEndpoint the end-session endpoint URL for the Admin UI web server; may be null to unset
     */
    public void setAuiWebServerEndSessionEndpoint(String auiWebServerEndSessionEndpoint) {
        this.auiWebServerEndSessionEndpoint = auiWebServerEndSessionEndpoint;
    }

    /**
     * Client identifier used by the Admin UI backend API server.
     *
     * @return the client identifier for the Admin UI backend API server, or `null` if not set
     */
    public String getAuiBackendApiServerClientId() {
        return auiBackendApiServerClientId;
    }

    /**
     * Sets the client identifier used by the Admin UI backend API server.
     *
     * @param auiBackendApiServerClientId the client ID for the Admin UI backend API server
     */
    public void setAuiBackendApiServerClientId(String auiBackendApiServerClientId) {
        this.auiBackendApiServerClientId = auiBackendApiServerClientId;
    }

    /**
     * Client secret for the Admin UI backend API server.
     *
     * @return the client secret, or null if not set
     */
    public String getAuiBackendApiServerClientSecret() {
        return auiBackendApiServerClientSecret;
    }

    /**
     * Sets the client secret used by the Admin UI Backend API server.
     *
     * @param auiBackendApiServerClientSecret the client secret for the backend API server
     */
    public void setAuiBackendApiServerClientSecret(String auiBackendApiServerClientSecret) {
        this.auiBackendApiServerClientSecret = auiBackendApiServerClientSecret;
    }

    /**
     * Get the OAuth2 scope configured for the Admin UI backend API server.
     *
     * @return the scope string used by the backend API server, or {@code null} if not set
     */
    public String getAuiBackendApiServerScope() {
        return auiBackendApiServerScope;
    }

    /**
     * Sets the OAuth2 scope string used by the Admin UI Backend API server.
     *
     * @param auiBackendApiServerScope the scope value to use when requesting tokens from the backend API server
     */
    public void setAuiBackendApiServerScope(String auiBackendApiServerScope) {
        this.auiBackendApiServerScope = auiBackendApiServerScope;
    }

    /**
     * Gets the ACR values used by the Admin UI backend API server.
     *
     * @return the ACR values for the Admin UI backend API server, or {@code null} if not set
     */
    public String getAuiBackendApiServerAcrValues() {
        return auiBackendApiServerAcrValues;
    }

    /**
     * Set the AUI Backend API server ACR values requested during authentication.
     *
     * @param auiBackendApiServerAcrValues space-separated ACR (Authentication Context Class Reference) values to request from the backend API server, or null to clear. 
     */
    public void setAuiBackendApiServerAcrValues(String auiBackendApiServerAcrValues) {
        this.auiBackendApiServerAcrValues = auiBackendApiServerAcrValues;
    }

    /**
     * Returns the redirect URL configured for the Admin UI backend API server.
     *
     * @return the redirect URL for the Admin UI backend API server, or {@code null} if not set
     */
    public String getAuiBackendApiServerRedirectUrl() {
        return auiBackendApiServerRedirectUrl;
    }

    /**
     * Set the redirect URL used by the Admin UI backend API server.
     *
     * @param auiBackendApiServerRedirectUrl the redirect URL for the Admin UI backend API server
     */
    public void setAuiBackendApiServerRedirectUrl(String auiBackendApiServerRedirectUrl) {
        this.auiBackendApiServerRedirectUrl = auiBackendApiServerRedirectUrl;
    }

    /**
     * Gets the Admin UI Backend API server front-channel logout URL.
     *
     * @return the front-channel logout URL for the Admin UI Backend API server, or {@code null} if not set.
     */
    public String getAuiBackendApiServerFrontChannelLogoutUrl() {
        return auiBackendApiServerFrontChannelLogoutUrl;
    }

    /**
     * Set the Admin UI backend API server front-channel logout URL.
     *
     * @param auiBackendApiServerFrontChannelLogoutUrl the front-channel logout URL for the Admin UI backend API server, or `null` to unset it
     */
    public void setAuiBackendApiServerFrontChannelLogoutUrl(String auiBackendApiServerFrontChannelLogoutUrl) {
        this.auiBackendApiServerFrontChannelLogoutUrl = auiBackendApiServerFrontChannelLogoutUrl;
    }

    /**
     * Post-logout redirect URI for the Admin UI backend API server.
     *
     * @return the post-logout redirect URI for the Admin UI backend API server, or {@code null} if not set
     */
    public String getAuiBackendApiServerPostLogoutRedirectUri() {
        return auiBackendApiServerPostLogoutRedirectUri;
    }

    /**
     * Set the Admin UI backend API server post-logout redirect URI.
     *
     * @param auiBackendApiServerPostLogoutRedirectUri the post-logout redirect URI to use for the Admin UI backend API server
     */
    public void setAuiBackendApiServerPostLogoutRedirectUri(String auiBackendApiServerPostLogoutRedirectUri) {
        this.auiBackendApiServerPostLogoutRedirectUri = auiBackendApiServerPostLogoutRedirectUri;
    }

    /**
     * Gets the authorization base URL for the Admin UI backend API server.
     *
     * @return the authorization base URL for the Admin UI backend API server, or `null` if not set
     */
    public String getAuiBackendApiServerAuthzBaseUrl() {
        return auiBackendApiServerAuthzBaseUrl;
    }

    /**
     * Sets the authorization server base URL used by the Admin UI backend API server.
     *
     * @param auiBackendApiServerAuthzBaseUrl the base authorization URL
     */
    public void setAuiBackendApiServerAuthzBaseUrl(String auiBackendApiServerAuthzBaseUrl) {
        this.auiBackendApiServerAuthzBaseUrl = auiBackendApiServerAuthzBaseUrl;
    }

    /**
     * Token endpoint URL for the Admin UI backend API server.
     *
     * @return the token endpoint URL for the Admin UI backend API server, or {@code null} if not configured
     */
    public String getAuiBackendApiServerTokenEndpoint() {
        return auiBackendApiServerTokenEndpoint;
    }

    /**
     * Set the token endpoint URL for the Admin UI backend API server.
     *
     * @param auiBackendApiServerTokenEndpoint the token endpoint URL
     */
    public void setAuiBackendApiServerTokenEndpoint(String auiBackendApiServerTokenEndpoint) {
        this.auiBackendApiServerTokenEndpoint = auiBackendApiServerTokenEndpoint;
    }

    /**
     * Provides the introspection endpoint URL for the Admin UI backend API server.
     *
     * @return the introspection endpoint URL, or {@code null} if not set
     */
    public String getAuiBackendApiServerIntrospectionEndpoint() {
        return auiBackendApiServerIntrospectionEndpoint;
    }

    /**
     * Sets the introspection endpoint URL used by the Admin UI backend API server.
     *
     * @param auiBackendApiServerIntrospectionEndpoint the introspection endpoint URL, or null to clear it
     */
    public void setAuiBackendApiServerIntrospectionEndpoint(String auiBackendApiServerIntrospectionEndpoint) {
        this.auiBackendApiServerIntrospectionEndpoint = auiBackendApiServerIntrospectionEndpoint;
    }

    /**
     * Retrieves the Admin UI backend API server UserInfo endpoint URL.
     *
     * @return the UserInfo endpoint URL for the Admin UI backend API server, or {@code null} if not configured
     */
    public String getAuiBackendApiServerUserInfoEndpoint() {
        return auiBackendApiServerUserInfoEndpoint;
    }

    /**
     * Set the Admin UI backend API server UserInfo endpoint URL.
     *
     * @param auiBackendApiServerUserInfoEndpoint the URL of the backend API server's UserInfo endpoint, or `null` to clear it
     */
    public void setAuiBackendApiServerUserInfoEndpoint(String auiBackendApiServerUserInfoEndpoint) {
        this.auiBackendApiServerUserInfoEndpoint = auiBackendApiServerUserInfoEndpoint;
    }

    /**
     * Gets the Admin UI backend API server end-session endpoint URL.
     *
     * @return the end-session endpoint URL for the Admin UI backend API server, or {@code null} if not set
     */
    public String getAuiBackendApiServerEndSessionEndpoint() {
        return auiBackendApiServerEndSessionEndpoint;
    }

    /**
     * Sets the Admin UI Backend API server end-session endpoint URL.
     *
     * @param auiBackendApiServerEndSessionEndpoint the end-session endpoint URL used by the Admin UI backend API
     */
    public void setAuiBackendApiServerEndSessionEndpoint(String auiBackendApiServerEndSessionEndpoint) {
        this.auiBackendApiServerEndSessionEndpoint = auiBackendApiServerEndSessionEndpoint;
    }

    /**
     * Retrieves the license configuration for the Admin UI.
     *
     * @return the LicenseConfiguration instance, or {@code null} if not set
     */
    public LicenseConfiguration getLicenseConfiguration() {
        return licenseConfiguration;
    }

    /**
     * Sets the license configuration used by the Admin UI.
     *
     * @param licenseConfiguration the license configuration to apply
     */
    public void setLicenseConfiguration(LicenseConfiguration licenseConfiguration) {
        this.licenseConfiguration = licenseConfiguration;
    }

    /**
     * Gets whether editing the SMTP keystore is allowed.
     *
     * @return `true` if SMTP keystore editing is allowed, `false` if it is disallowed, or `null` if unset.
     */
    public Boolean getAllowSmtpKeystoreEdit() {
        return allowSmtpKeystoreEdit;
    }

    /**
     * Set whether editing the SMTP keystore is allowed.
     *
     * @param allowSmtpKeystoreEdit `true` to allow SMTP keystore edits, `false` to disallow; may be `null` to unset the value
     */
    public void setAllowSmtpKeystoreEdit(Boolean allowSmtpKeystoreEdit) {
        this.allowSmtpKeystoreEdit = allowSmtpKeystoreEdit;
    }

    /**
     * Gets the configured Cedarling logging type.
     *
     * @return the configured {@link CedarlingLogType}, or {@code null} if none is set
     */
    public CedarlingLogType getCedarlingLogType() {
        return cedarlingLogType;
    }

    /**
     * Sets the Cedarling logging type for this configuration.
     *
     * @param cedarlingLogType the CedarlingLogType to apply; may be null to unset the value
     */
    public void setCedarlingLogType(CedarlingLogType cedarlingLogType) {
        this.cedarlingLogType = cedarlingLogType;
    }

    /**
     * Gets the Admin UI Cedarling policy store URL.
     *
     * @return the configured Cedarling policy store URL for the Admin UI, or {@code null} if not set.
     */
    public String getAuiCedarlingPolicyStoreUrl() {
        return auiCedarlingPolicyStoreUrl;
    }

    /**
     * Sets the Admin UI Cedarling policy store URL.
     *
     * @param auiCedarlingPolicyStoreUrl the URL of the Cedarling policy store used by the Admin UI
     */
    public void setAuiCedarlingPolicyStoreUrl(String auiCedarlingPolicyStoreUrl) {
        this.auiCedarlingPolicyStoreUrl = auiCedarlingPolicyStoreUrl;
    }

    /**
     * Gets the default filesystem path used for the Admin UI's Cedarling policy store.
     *
     * @return the default Cedarling policy store path for the Admin UI, or `null` if not set
     */
    public String getAuiCedarlingDefaultPolicyStorePath() {
        return auiCedarlingDefaultPolicyStorePath;
    }

    /**
     * Sets the default filesystem path used by Cedarling for the Admin UI policy store.
     *
     * @param auiCedarlingDefaultPolicyStorePath the default policy store path to use, or `null` to unset
     */
    public void setAuiCedarlingDefaultPolicyStorePath(String auiCedarlingDefaultPolicyStorePath) {
        this.auiCedarlingDefaultPolicyStorePath = auiCedarlingDefaultPolicyStorePath;
    }

    /**
     * Returns the configured Cedarling policy store retrieval point.
     *
     * @return the current CedarlingPolicyStrRetrievalPoint; never null — defaults to {@code CedarlingPolicyStrRetrievalPoint.DEFAULT} when not explicitly set.
     */
    public CedarlingPolicyStrRetrievalPoint getCedarlingPolicyStoreRetrievalPoint() {
        return cedarlingPolicyStoreRetrievalPoint;
    }

    /**
     * Set the cedarling policy store retrieval point for this configuration.
     *
     * @param cedarlingPolicyStoreRetrievalPoint the retrieval point to set; if `null`, the value will be set to {@link CedarlingPolicyStrRetrievalPoint#DEFAULT}
     */
    public void setCedarlingPolicyStoreRetrievalPoint(CedarlingPolicyStrRetrievalPoint cedarlingPolicyStoreRetrievalPoint) {
        this.cedarlingPolicyStoreRetrievalPoint = cedarlingPolicyStoreRetrievalPoint != null
                ? cedarlingPolicyStoreRetrievalPoint
                : CedarlingPolicyStrRetrievalPoint.DEFAULT;
    }

    /**
     * Produce a string representation of this AUIConfiguration containing all configuration fields and their current values.
     *
     * @return a string containing each field name and its current value for this configuration instance
     */
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
                ", cedarlingPolicyStoreRetrievalPoint=" + cedarlingPolicyStoreRetrievalPoint +
                '}';
    }
}

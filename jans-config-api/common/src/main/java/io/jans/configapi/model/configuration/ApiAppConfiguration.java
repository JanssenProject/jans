package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiAppConfiguration implements Configuration {

    private boolean configOauthEnabled;
    private boolean disableLoggerTimer;
    private boolean disableAuditLogger;
    private boolean customAttributeValidationEnabled;
    private List<String> apiApprovedIssuer;
    private String apiProtectionType;
    private String apiClientId;
    private String apiClientPassword;

    private boolean endpointInjectionEnabled;
    private String authIssuerUrl;
    private String authOpenidConfigurationUrl;
    private String authOpenidIntrospectionUrl;
    private String authOpenidTokenUrl;
    private String authOpenidRevokeUrl;

    private List<String> exclusiveAuthScopes;

    private List<CorsConfigurationFilter> corsConfigurationFilters;

    private String loggingLevel;
    private String loggingLayout;
    private String externalLoggerConfiguration;
    private Boolean disableJdkLogger = true;
    private int maxCount;
    
    private List<String> userExclusionAttributes;
    private List<String> userMandatoryAttributes;
    private AgamaConfiguration agamaConfiguration;
    private AuditLogConf auditLogConf;
    private DataFormatConversionConf dataFormatConversionConf;
    private List<PluginConf> plugins;
    
    private AssetMgtConfiguration assetMgtConfiguration;

    public boolean isConfigOauthEnabled() {
        return configOauthEnabled;
    }

    public void setConfigOauthEnabled(boolean configOauthEnabled) {
        this.configOauthEnabled = configOauthEnabled;
    }

    public boolean isDisableLoggerTimer() {
        return disableLoggerTimer;
    }

    public void setDisableLoggerTimer(boolean disableLoggerTimer) {
        this.disableLoggerTimer = disableLoggerTimer;
    }

    public boolean isDisableAuditLogger() {
        return disableAuditLogger;
    }

    public void setDisableAuditLogger(boolean disableAuditLogger) {
        this.disableAuditLogger = disableAuditLogger;
    }

    public boolean isCustomAttributeValidationEnabled() {
        return customAttributeValidationEnabled;
    }

    public void setCustomAttributeValidationEnabled(boolean customAttributeValidationEnabled) {
        this.customAttributeValidationEnabled = customAttributeValidationEnabled;
    }

    public List<String> getApiApprovedIssuer() {
        return apiApprovedIssuer;
    }

    public void setApiApprovedIssuer(List<String> apiApprovedIssuer) {
        this.apiApprovedIssuer = apiApprovedIssuer;
    }

    public String getApiProtectionType() {
        return apiProtectionType;
    }

    public void setApiProtectionType(String apiProtectionType) {
        this.apiProtectionType = apiProtectionType;
    }

    public String getApiClientId() {
        return apiClientId;
    }

    public void setApiClientId(String apiClientId) {
        this.apiClientId = apiClientId;
    }

    public String getApiClientPassword() {
        return apiClientPassword;
    }

    public void setApiClientPassword(String apiClientPassword) {
        this.apiClientPassword = apiClientPassword;
    }

    public boolean isEndpointInjectionEnabled() {
        return endpointInjectionEnabled;
    }

    public void setEndpointInjectionEnabled(boolean endpointInjectionEnabled) {
        this.endpointInjectionEnabled = endpointInjectionEnabled;
    }

    public String getAuthIssuerUrl() {
        return authIssuerUrl;
    }

    public void setAuthIssuerUrl(String authIssuerUrl) {
        this.authIssuerUrl = authIssuerUrl;
    }

    public String getAuthOpenidConfigurationUrl() {
        return authOpenidConfigurationUrl;
    }

    public void setAuthOpenidConfigurationUrl(String authOpenidConfigurationUrl) {
        this.authOpenidConfigurationUrl = authOpenidConfigurationUrl;
    }

    public String getAuthOpenidIntrospectionUrl() {
        return authOpenidIntrospectionUrl;
    }

    public void setAuthOpenidIntrospectionUrl(String authOpenidIntrospectionUrl) {
        this.authOpenidIntrospectionUrl = authOpenidIntrospectionUrl;
    }

    public String getAuthOpenidTokenUrl() {
        return authOpenidTokenUrl;
    }

    public void setAuthOpenidTokenUrl(String authOpenidTokenUrl) {
        this.authOpenidTokenUrl = authOpenidTokenUrl;
    }

    public String getAuthOpenidRevokeUrl() {
        return authOpenidRevokeUrl;
    }

    public void setAuthOpenidRevokeUrl(String authOpenidRevokeUrl) {
        this.authOpenidRevokeUrl = authOpenidRevokeUrl;
    }

    public List<String> getExclusiveAuthScopes() {
        return exclusiveAuthScopes;
    }

    public void setExclusiveAuthScopes(List<String> exclusiveAuthScopes) {
        this.exclusiveAuthScopes = exclusiveAuthScopes;
    }

    public List<CorsConfigurationFilter> getCorsConfigurationFilters() {
        return corsConfigurationFilters;
    }

    public void setCorsConfigurationFilters(List<CorsConfigurationFilter> corsConfigurationFilters) {
        this.corsConfigurationFilters = corsConfigurationFilters;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public String getLoggingLayout() {
        return loggingLayout;
    }

    public void setLoggingLayout(String loggingLayout) {
        this.loggingLayout = loggingLayout;
    }

    public String getExternalLoggerConfiguration() {
        return externalLoggerConfiguration;
    }

    public void setExternalLoggerConfiguration(String externalLoggerConfiguration) {
        this.externalLoggerConfiguration = externalLoggerConfiguration;
    }

    public Boolean getDisableJdkLogger() {
        return disableJdkLogger;
    }

    public void setDisableJdkLogger(Boolean disableJdkLogger) {
        this.disableJdkLogger = disableJdkLogger;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public List<String> getUserExclusionAttributes() {
        return userExclusionAttributes;
    }

    public void setUserExclusionAttributes(List<String> userExclusionAttributes) {
        this.userExclusionAttributes = userExclusionAttributes;
    }

    public List<String> getUserMandatoryAttributes() {
        return userMandatoryAttributes;
    }

    public void setUserMandatoryAttributes(List<String> userMandatoryAttributes) {
        this.userMandatoryAttributes = userMandatoryAttributes;
    }

    public AgamaConfiguration getAgamaConfiguration() {
        return agamaConfiguration;
    }

    public void setAgamaConfiguration(AgamaConfiguration agamaConfiguration) {
        this.agamaConfiguration = agamaConfiguration;
    }

    public AuditLogConf getAuditLogConf() {
        return auditLogConf;
    }

    public void setAuditLogConf(AuditLogConf auditLogConf) {
        this.auditLogConf = auditLogConf;
    }

    public DataFormatConversionConf getDataFormatConversionConf() {
        return dataFormatConversionConf;
    }

    public void setDataFormatConversionConf(DataFormatConversionConf dataFormatConversionConf) {
        this.dataFormatConversionConf = dataFormatConversionConf;
    }

    public List<PluginConf> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<PluginConf> plugins) {
        this.plugins = plugins;
    }

    public AssetMgtConfiguration getAssetMgtConfiguration() {
        return assetMgtConfiguration;
    }

    public void setAssetMgtConfiguration(AssetMgtConfiguration assetMgtConfiguration) {
        this.assetMgtConfiguration = assetMgtConfiguration;
    }

    @Override
    public String toString() {
        return "ApiAppConfiguration [configOauthEnabled=" + configOauthEnabled + ", disableLoggerTimer="
                + disableLoggerTimer + ", disableAuditLogger=" + disableAuditLogger
                + ", customAttributeValidationEnabled=" + customAttributeValidationEnabled + ", apiApprovedIssuer="
                + apiApprovedIssuer + ", apiProtectionType=" + apiProtectionType + ", apiClientId=" + apiClientId
                + ", apiClientPassword=" + apiClientPassword + ", endpointInjectionEnabled=" + endpointInjectionEnabled
                + ", authIssuerUrl=" + authIssuerUrl + ", authOpenidConfigurationUrl=" + authOpenidConfigurationUrl
                + ", authOpenidIntrospectionUrl=" + authOpenidIntrospectionUrl + ", authOpenidTokenUrl="
                + authOpenidTokenUrl + ", authOpenidRevokeUrl=" + authOpenidRevokeUrl 
                + ", exclusiveAuthScopes=" + exclusiveAuthScopes
                + ", corsConfigurationFilters=" + corsConfigurationFilters + ", loggingLevel=" + loggingLevel
                + ", loggingLayout=" + loggingLayout + ", externalLoggerConfiguration=" + externalLoggerConfiguration
                + ", disableJdkLogger=" + disableJdkLogger + ", maxCount=" + maxCount + ", userExclusionAttributes="
                + userExclusionAttributes + ", userMandatoryAttributes=" + userMandatoryAttributes
                + ", agamaConfiguration=" + agamaConfiguration + ", auditLogConf=" + auditLogConf
                + ", dataFormatConversionConf=" + dataFormatConversionConf + ", plugins=" + plugins
                + ", assetMgtConfiguration=" + assetMgtConfiguration + "]";
    }
        
}


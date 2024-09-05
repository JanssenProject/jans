package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;
import io.jans.configapi.util.ApiConstants;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiAppConfiguration implements Configuration {
    
    @Schema(description = "Config API service name.")
    private String serviceName;

    @Schema(description = "OAuth authentication enable/disable flag. Default value `true`.")
    private boolean configOauthEnabled;

    @Schema(description = "Flag to enable/disable timer to dynamically reflect log configuration changes. Default value `true`Default value `false`.")
    private boolean disableLoggerTimer;

    @Schema(description = "Flag to enable/disable request audit. Default value `false`.")
    private boolean disableAuditLogger;

    @Schema(description = "Flag to enable/disable check if custom attribue is declared in schema. Default value `true`.")
    private boolean customAttributeValidationEnabled;

    @Schema(description = "Flag to enable/disable check if acr customScript is enabled. Default value `true`.")
    private boolean acrValidationEnabled;

    @Schema(description = "List of approved external Auth server to validate token.")
    private List<String> apiApprovedIssuer;

    @Schema(description = "Name of supported API protection mechansim. Supported type is `OAuth2`.")
    private String apiProtectionType;

    @Schema(description = "Config-API client ID.")
    private String apiClientId;

    @Schema(description = "Config-API client password.")
    private String apiClientPassword;

    private boolean endpointInjectionEnabled;

    @Schema(description = "Issuer Identifier of Jans OpenID Connect Provider.")
    private String authIssuerUrl;

    @Schema(description = "Jans OpenID Connect Provider Well-Known Configuration URL.")
    private String authOpenidConfigurationUrl;

    @Schema(description = "Jans URL of the OpenID Connect Provider's OAuth 2.0 Authorization Endpoint.")
    private String authOpenidIntrospectionUrl;

    @Schema(description = "Jans URL of the OpenID Connect Provider's OAuth 2.0 Token Endpoint.")
    private String authOpenidTokenUrl;

    @Schema(description = "Jans URL of the OpenID Connect Provider's OAuth 2.0 Revoke Token Endpoint.")
    private String authOpenidRevokeUrl;

    @Schema(description = "List of oAuth scope that can be validity for an access tokens only by underlying Jans Auth server.")
    private List<String> exclusiveAuthScopes;

    @Schema(description = "CORS configuration filter properties.")
    private List<CorsConfigurationFilter> corsConfigurationFilters;

    @Schema(description = "Specify logging level of Loggers. Default level is `INFO`.")
    private String loggingLevel;

    @Schema(description = "Log4j logging layout. Default value `TEXT`.")
    private String loggingLayout;

    @Schema(description = "The path to the external log4j2 logging configuration.")
    private String externalLoggerConfiguration;

    @Schema(description = "Choose whether to disable JDK loggers.")
    private Boolean disableJdkLogger = true;

    @Schema(description = "Maximum number of results per page in search endpoints.")
    private int maxCount;

    @Schema(description = "List of ACR values that should be excluded from active validation check.")
    private List<String> acrExclusionList;
    
    @Schema(description = "User attribute that should not be returned in response.")
    private List<String> userExclusionAttributes;

    @Schema(description = "List of User mandatory attribute for user creation request.")
    private List<String> userMandatoryAttributes;

    @Schema(description = "Agama configuration details.")
    private AgamaConfiguration agamaConfiguration;

    @Schema(description = "Audit Log configuration details.")
    private AuditLogConf auditLogConf;

    @Schema(description = "Configuration for data-type converstion.")
    private DataFormatConversionConf dataFormatConversionConf;

    @Schema(description = "Details of enabled plugins.")
    private List<PluginConf> plugins;

    @Schema(description = "Asset management configuration details.")
    private AssetMgtConfiguration assetMgtConfiguration;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

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

    public boolean isAcrValidationEnabled() {
        return acrValidationEnabled;
    }

    public void setAcrValidationEnabled(boolean acrValidationEnabled) {
        this.acrValidationEnabled = acrValidationEnabled;
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
        if(this.maxCount<=0) {
            this.maxCount = ApiConstants.DEFAULT_MAX_COUNT;
        }
    }

    public List<String> getAcrExclusionList() {
        return acrExclusionList;
    }

    public void setAcrExclusionList(List<String> acrExclusionList) {
        this.acrExclusionList = acrExclusionList;
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
        return "ApiAppConfiguration [serviceName=" + serviceName + ", configOauthEnabled=" + configOauthEnabled
                + ", disableLoggerTimer=" + disableLoggerTimer + ", disableAuditLogger=" + disableAuditLogger
                + ", customAttributeValidationEnabled=" + customAttributeValidationEnabled + ", acrValidationEnabled="
                + acrValidationEnabled + ", apiApprovedIssuer=" + apiApprovedIssuer + ", apiProtectionType="
                + apiProtectionType + ", apiClientId=" + apiClientId 
                + ", endpointInjectionEnabled=" + endpointInjectionEnabled + ", authIssuerUrl=" + authIssuerUrl
                + ", authOpenidConfigurationUrl=" + authOpenidConfigurationUrl + ", authOpenidIntrospectionUrl="
                + authOpenidIntrospectionUrl + ", authOpenidTokenUrl=" + authOpenidTokenUrl + ", authOpenidRevokeUrl="
                + authOpenidRevokeUrl + ", exclusiveAuthScopes=" + exclusiveAuthScopes + ", corsConfigurationFilters="
                + corsConfigurationFilters + ", loggingLevel=" + loggingLevel + ", loggingLayout=" + loggingLayout
                + ", externalLoggerConfiguration=" + externalLoggerConfiguration + ", disableJdkLogger="
                + disableJdkLogger + ", maxCount=" + maxCount + ", acrExclusionList=" + acrExclusionList
                + ", userExclusionAttributes=" + userExclusionAttributes + ", userMandatoryAttributes="
                + userMandatoryAttributes + ", agamaConfiguration=" + agamaConfiguration + ", auditLogConf="
                + auditLogConf + ", dataFormatConversionConf=" + dataFormatConversionConf + ", plugins=" + plugins
                + ", assetMgtConfiguration=" + assetMgtConfiguration + "]";
    }    
    
}

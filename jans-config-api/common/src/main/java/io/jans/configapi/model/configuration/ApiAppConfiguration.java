package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiAppConfiguration implements Configuration {

    private boolean configOauthEnabled;
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

    private String smallryeHealthRootPath;
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

    public boolean isConfigOauthEnabled() {
        return configOauthEnabled;
    }

    public void setConfigOauthEnabled(boolean configOauthEnabled) {
        this.configOauthEnabled = configOauthEnabled;
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

    public String getSmallryeHealthRootPath() {
        return smallryeHealthRootPath;
    }

    public void setSmallryeHealthRootPath(String smallryeHealthRootPath) {
        this.smallryeHealthRootPath = smallryeHealthRootPath;
    }

    public List<CorsConfigurationFilter> getCorsConfigurationFilters() {
        if (corsConfigurationFilters == null) {
            corsConfigurationFilters = new ArrayList<>();
        }

        return corsConfigurationFilters;
    }

    public void setCorsConfigurationFilters(List<CorsConfigurationFilter> corsConfigurationFilters) {
        if (corsConfigurationFilters == null) {
            this.corsConfigurationFilters = new ArrayList<>();
        } else {
            this.corsConfigurationFilters = new ArrayList<>();
            this.corsConfigurationFilters.addAll(corsConfigurationFilters);
        }
    }

    public List<String> getExclusiveAuthScopes() {
        if (exclusiveAuthScopes == null) {
            exclusiveAuthScopes = new ArrayList<>();
        }
        return exclusiveAuthScopes;
    }

    public void setExclusiveAuthScopes(List<String> exclusiveAuthScopes) {
        this.exclusiveAuthScopes = exclusiveAuthScopes;
        if (exclusiveAuthScopes == null) {
            this.exclusiveAuthScopes = new ArrayList<>();
        } else {
            this.exclusiveAuthScopes = new ArrayList<>();
            this.exclusiveAuthScopes.addAll(exclusiveAuthScopes);
        }
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
        return this.maxCount;
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

    @Override
    public String toString() {
        return "ApiAppConfiguration [" + " apiApprovedIssuer=" + apiApprovedIssuer + ", apiProtectionType="
                + apiProtectionType + ", apiClientId=" + apiClientId + ", apiClientPassword=" + apiClientPassword
                + ", endpointInjectionEnabled=" + endpointInjectionEnabled + ", authIssuerUrl=" + authIssuerUrl
                + ", authOpenidConfigurationUrl=" + authOpenidConfigurationUrl + ", authOpenidIntrospectionUrl="
                + authOpenidIntrospectionUrl + ", authOpenidTokenUrl=" + authOpenidTokenUrl + ", authOpenidRevokeUrl="
                + authOpenidRevokeUrl + ", smallryeHealthRootPath=" + smallryeHealthRootPath
                + ", corsConfigurationFilters=" + corsConfigurationFilters + ", exclusiveAuthScopes="
                + exclusiveAuthScopes + ", loggingLevel=" + loggingLevel + " , loggingLayout=" + loggingLayout
                + " , externalLoggerConfiguration=" + externalLoggerConfiguration + " , disableJdkLogger="
                + disableJdkLogger + " , maxCount =" + maxCount
                + " , userExclusionAttributes="+ userExclusionAttributes
                + " , userMandatoryAttributes="+ userMandatoryAttributes
                + " , agamaConfiguration="+ agamaConfiguration
                + " , auditLogConf="+ auditLogConf
                + " , dataFormatConversionConf="+ dataFormatConversionConf
                + " , plugins="+ plugins
                + "]";
    }

}


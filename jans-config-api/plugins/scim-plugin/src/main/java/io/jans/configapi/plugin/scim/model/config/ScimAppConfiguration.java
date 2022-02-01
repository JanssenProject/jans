package io.jans.configapi.plugin.scim.model.config;

import io.jans.config.oxtrust.Configuration;
import io.jans.orm.annotation.AttributeName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimAppConfiguration implements Configuration, Serializable {

    private static final long serialVersionUID = 7620150845020446557L;
    
    @JsonProperty("baseDN")
    @AttributeName(name = "baseDN")
    private String baseDn;
        
    private String applicationUrl;
    private String baseEndpoint;
    private String personCustomObjectClass;
    
    @AttributeName(name = "oxAuthIssuer")
    private String authIssuer;
    
    private String protectionMode;
    private int maxCount;
    private String userExtensionSchemaURI;
    
    private String loggingLevel;
    private String loggingLayout;
    private String externalLoggerConfiguration;
    
    @JsonProperty("metricReporterInterval")
    @AttributeName(name = "metricReporterInterval")
    private int metricReportInterval;
    
    @JsonProperty("metricReporterKeepDataDays")
    @AttributeName(name = "metricReporterKeepDataDays")
    private int metricReportKeepDataDays;
    
    @JsonProperty("metricReporterEnabled")
    @AttributeName(name = "metricReporterEnabled")
    private Boolean metricReportEnabled;
    
    private Boolean disableJdkLogger = true;
    private Boolean useLocalCache = false;


    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    public String getPersonCustomObjectClass() {
        return personCustomObjectClass;
    }

    public void setPersonCustomObjectClass(String personCustomObjectClass) {
        this.personCustomObjectClass = personCustomObjectClass;
    }

    public String getAuthIssuer() {
        return authIssuer;
    }

    public void setAuthIssuer(String authIssuer) {
        this.authIssuer = authIssuer;
    }

    public String getProtectionMode() {
        return protectionMode;
    }

    public void setProtectionMode(String protectionMode) {
        this.protectionMode = protectionMode;
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getUserExtensionSchemaURI() {
        return userExtensionSchemaURI;
    }

    public void setUserExtensionSchemaURI(String userExtensionSchemaURI) {
        this.userExtensionSchemaURI = userExtensionSchemaURI;
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

    public int getMetricReportInterval() {
        return metricReportInterval;
    }

    public void setMetricReportInterval(int metricReportInterval) {
        this.metricReportInterval = metricReportInterval;
    }

    public int getMetricReportKeepDataDays() {
        return metricReportKeepDataDays;
    }

    public void setMetricReportKeepDataDays(int metricReportKeepDataDays) {
        this.metricReportKeepDataDays = metricReportKeepDataDays;
    }

    public Boolean getMetricReportEnabled() {
        return metricReportEnabled;
    }

    public void setMetricReportEnabled(Boolean metricReportEnabled) {
        this.metricReportEnabled = metricReportEnabled;
    }

    public Boolean getDisableJdkLogger() {
        return disableJdkLogger;
    }

    public void setDisableJdkLogger(Boolean disableJdkLogger) {
        this.disableJdkLogger = disableJdkLogger;
    }

    public Boolean getUseLocalCache() {
        return useLocalCache;
    }

    public void setUseLocalCache(Boolean useLocalCache) {
        this.useLocalCache = useLocalCache;
    }

    @Override
    public String toString() {
        return "ScimAppConfiguration [baseDn=" + baseDn + ", applicationUrl=" + applicationUrl + ", baseEndpoint="
                + baseEndpoint + ", personCustomObjectClass=" + personCustomObjectClass + ", authIssuer="
                + authIssuer + ", protectionMode=" + protectionMode + ", maxCount=" + maxCount
                + ", userExtensionSchemaURI=" + userExtensionSchemaURI + ", loggingLevel=" + loggingLevel
                + ", loggingLayout=" + loggingLayout + ", externalLoggerConfiguration=" + externalLoggerConfiguration
                + ", metricReportInterval=" + metricReportInterval + ", metricReportKeepDataDays="
                + metricReportKeepDataDays + ", metricReportEnabled=" + metricReportEnabled
                + ", disableJdkLogger=" + disableJdkLogger + ", useLocalCache=" + useLocalCache + "]";
    }

}

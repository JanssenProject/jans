package io.jans.configapi.plugin.scim.model.config;

import io.jans.config.oxtrust.Configuration;
import io.jans.orm.annotation.AttributeName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimAppConfiguration implements Configuration, Serializable {

    private static final long serialVersionUID = 1244429172476500948L;

    @JsonProperty("baseDN")
    @AttributeName(name = "baseDN")
    private String baseDn;

    @JsonProperty("applicationUrl")
    @AttributeName(name = "applicationUrl")
    private String appUrl;

    @JsonProperty("baseEndpoint")
    @AttributeName(name = "baseEndpoint")
    private String baseUrl;

    @JsonProperty("personCustomObjectClass")
    @AttributeName(name = "personCustomObjectClass")
    private String personCustomObjClass;

    @JsonProperty("oxAuthIssuer")
    @AttributeName(name = "oxAuthIssuer")
    private String authIssuer;

    @JsonProperty("protectionMode")
    @AttributeName(name = "protectionMode")
    private String protectionScimMode;

    private int maxCount;

    @JsonProperty("userExtensionSchemaURI")
    @AttributeName(name = "userExtensionSchemaURI")
    private String userExtSchemaURI;

    @JsonProperty("loggingLevel")
    @AttributeName(name = "loggingLevel")
    private String logLevel;

    @JsonProperty("loggingLayout")
    @AttributeName(name = "loggingLayout")
    private String logLayout;

    @JsonProperty("externalLoggerConfiguration")
    @AttributeName(name = "externalLoggerConfiguration")
    private String externalLoggerConfig;

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

    @JsonProperty("bulkMaxOperations")
    @AttributeName(name = "bulkMaxOperations")
    private int bulkMaxOperations;

    @JsonProperty("bulkMaxPayloadSize")
    @AttributeName(name = "bulkMaxPayloadSize")
    private long bulkMaxPayloadSize;

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPersonCustomObjClass() {
        return personCustomObjClass;
    }

    public void setPersonCustomObjClass(String personCustomObjClass) {
        this.personCustomObjClass = personCustomObjClass;
    }

    public String getAuthIssuer() {
        return authIssuer;
    }

    public void setAuthIssuer(String authIssuer) {
        this.authIssuer = authIssuer;
    }

    public String getProtectionScimMode() {
        return protectionScimMode;
    }

    public void setProtectionScimMode(String protectionScimMode) {
        this.protectionScimMode = protectionScimMode;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getUserExtSchemaURI() {
        return userExtSchemaURI;
    }

    public void setUserExtSchemaURI(String userExtSchemaURI) {
        this.userExtSchemaURI = userExtSchemaURI;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogLayout() {
        return logLayout;
    }

    public void setLogLayout(String logLayout) {
        this.logLayout = logLayout;
    }

    public String getExternalLoggerConfig() {
        return externalLoggerConfig;
    }

    public void setExternalLoggerConfig(String externalLoggerConfig) {
        this.externalLoggerConfig = externalLoggerConfig;
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
    
    public int getBulkMaxOperations() {
        return bulkMaxOperations;
    }

    public void setBulkMaxOperations(int bulkMaxOperations) {
        this.bulkMaxOperations = bulkMaxOperations;
    }

    public long getBulkMaxPayloadSize() {
        return bulkMaxPayloadSize;
    }

    public void setBulkMaxPayloadSize(long bulkMaxPayloadSize) {
        this.bulkMaxPayloadSize = bulkMaxPayloadSize;
    }

    @Override
    public String toString() {
        return "ScimAppConfiguration [baseDn=" + baseDn + ", appUrl=" + appUrl + ", baseUrl=" + baseUrl
                + ", personCustomObjClass=" + personCustomObjClass + ", authIssuer=" + authIssuer
                + ", protectionScimMode=" + protectionScimMode + ", maxCount=" + maxCount + ", userExtSchemaURI="
                + userExtSchemaURI + ", logLevel=" + logLevel + ", logLayout=" + logLayout + ", externalLoggerConfig="
                + externalLoggerConfig + ", metricReportInterval=" + metricReportInterval
                + ", metricReportKeepDataDays=" + metricReportKeepDataDays + ", metricReportEnabled="
                + metricReportEnabled + ", disableJdkLogger=" + disableJdkLogger + ", useLocalCache=" + useLocalCache
                + ", bulkMaxOperations=" + bulkMaxOperations + ", bulkMaxPayloadSize=" + bulkMaxPayloadSize + "]";
    }
    
}

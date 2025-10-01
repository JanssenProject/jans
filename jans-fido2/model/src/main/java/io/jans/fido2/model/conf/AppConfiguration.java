/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.as.model.configuration.Configuration;
import io.jans.doc.annotation.DocProperty;
import jakarta.enterprise.inject.Vetoed;
/**
 * Represents the configuration JSON file.
 *
 * @author Yuriy Movchan
 * @version May 13, 2020
 */


@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8558798638575129572L;

	@DocProperty(description = "URL using the https scheme for Issuer identifier")
    private String issuer;
	
	@DocProperty(description = "The base URL for Fido2 endpoints")
    private String baseEndpoint;
	
	@DocProperty(description = "Time interval for the Clean Service in seconds")
    private int cleanServiceInterval;
	
	@DocProperty(description = "Each clean up iteration fetches chunk of expired data per base dn and removes it from storage")
    private int cleanServiceBatchChunkSize = 100;
	
	@DocProperty(description = "Boolean value to indicate if Local Cache is to be used")
    private boolean useLocalCache;
	
	@DocProperty(description = "Boolean value specifying whether to enable JDK Loggers")
    private boolean disableJdkLogger = true;
	
	@DocProperty(description = "Logging level for Fido2 logger")
    private String loggingLevel;
	
	@DocProperty(description = "Logging layout used for Fido2")
    private String loggingLayout;
	
	@DocProperty(description = "Path to external Fido2 logging configuration")
    private String externalLoggerConfiguration;
	
	@DocProperty(description = "The interval for metric reporter in seconds")
    private int metricReporterInterval;
	
	@DocProperty(description = "The days to keep report data")
    private int metricReporterKeepDataDays;
	
	@DocProperty(description = "Boolean value specifying whether metric reporter is enabled")
    private boolean metricReporterEnabled = true;
	
	@DocProperty(description = "Boolean value specifying whether FIDO2 passkey metrics collection is enabled", defaultValue = "true")
    private boolean fido2MetricsEnabled = true;
	
	@DocProperty(description = "Number of days to keep FIDO2 passkey metrics data", defaultValue = "90")
    private int fido2MetricsRetentionDays = 90;
	
	@DocProperty(description = "Boolean value specifying whether to collect device information in FIDO2 metrics", defaultValue = "true")
    private boolean fido2DeviceInfoCollection = true;
	
	@DocProperty(description = "Boolean value specifying whether to categorize errors in FIDO2 metrics", defaultValue = "true")
    private boolean fido2ErrorCategorization = true;
	
	@DocProperty(description = "Boolean value specifying whether to collect detailed performance metrics for FIDO2 operations", defaultValue = "true")
    private boolean fido2PerformanceMetrics = true;
	
	@DocProperty(description = "Boolean value specifying whether FIDO2 metrics aggregation is enabled", defaultValue = "true")
    private boolean fido2MetricsAggregationEnabled = true;
	
	@DocProperty(description = "Interval in minutes for FIDO2 metrics aggregation", defaultValue = "60")
    private int fido2MetricsAggregationInterval = 60;
	
	@DocProperty(description = "Custom object class list for dynamic person enrolment")
    private List<String> personCustomObjectClassList;
	
	

	


    private Fido2Configuration fido2Configuration;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getBaseEndpoint() {
		return baseEndpoint;
	}

	public void setBaseEndpoint(String baseEndpoint) {
		this.baseEndpoint = baseEndpoint;
	}

	public int getCleanServiceInterval() {
		return cleanServiceInterval;
	}

	public void setCleanServiceInterval(int cleanServiceInterval) {
		this.cleanServiceInterval = cleanServiceInterval;
	}

	public int getCleanServiceBatchChunkSize() {
		return cleanServiceBatchChunkSize;
	}

	public void setCleanServiceBatchChunkSize(int cleanServiceBatchChunkSize) {
		this.cleanServiceBatchChunkSize = cleanServiceBatchChunkSize;
	}

	public boolean isUseLocalCache() {
		return useLocalCache;
	}

	public void setUseLocalCache(boolean useLocalCache) {
		this.useLocalCache = useLocalCache;
	}

	public Boolean getDisableJdkLogger() {
		return disableJdkLogger;
	}

	public void setDisableJdkLogger(Boolean disableJdkLogger) {
		this.disableJdkLogger = disableJdkLogger;
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

	public int getMetricReporterInterval() {
		return metricReporterInterval;
	}

	public void setMetricReporterInterval(int metricReporterInterval) {
		this.metricReporterInterval = metricReporterInterval;
	}

	public int getMetricReporterKeepDataDays() {
		return metricReporterKeepDataDays;
	}

	public void setMetricReporterKeepDataDays(int metricReporterKeepDataDays) {
		this.metricReporterKeepDataDays = metricReporterKeepDataDays;
	}

	public boolean getMetricReporterEnabled() {
		return metricReporterEnabled;
	}

	public void setMetricReporterEnabled(boolean metricReporterEnabled) {
		this.metricReporterEnabled = metricReporterEnabled;
	}

	public boolean isFido2MetricsEnabled() {
		return fido2MetricsEnabled;
	}

	public void setFido2MetricsEnabled(boolean fido2MetricsEnabled) {
		this.fido2MetricsEnabled = fido2MetricsEnabled;
	}

	public int getFido2MetricsRetentionDays() {
		return fido2MetricsRetentionDays;
	}

	public void setFido2MetricsRetentionDays(int fido2MetricsRetentionDays) {
		this.fido2MetricsRetentionDays = fido2MetricsRetentionDays;
	}

	public boolean isFido2DeviceInfoCollection() {
		return fido2DeviceInfoCollection;
	}

	public void setFido2DeviceInfoCollection(boolean fido2DeviceInfoCollection) {
		this.fido2DeviceInfoCollection = fido2DeviceInfoCollection;
	}

	public boolean isFido2ErrorCategorization() {
		return fido2ErrorCategorization;
	}

	public void setFido2ErrorCategorization(boolean fido2ErrorCategorization) {
		this.fido2ErrorCategorization = fido2ErrorCategorization;
	}

	public boolean isFido2PerformanceMetrics() {
		return fido2PerformanceMetrics;
	}

	public void setFido2PerformanceMetrics(boolean fido2PerformanceMetrics) {
		this.fido2PerformanceMetrics = fido2PerformanceMetrics;
	}

	public boolean isFido2MetricsAggregationEnabled() {
		return fido2MetricsAggregationEnabled;
	}

	public void setFido2MetricsAggregationEnabled(boolean fido2MetricsAggregationEnabled) {
		this.fido2MetricsAggregationEnabled = fido2MetricsAggregationEnabled;
	}

	public int getFido2MetricsAggregationInterval() {
		return fido2MetricsAggregationInterval;
	}

	public void setFido2MetricsAggregationInterval(int fido2MetricsAggregationInterval) {
		this.fido2MetricsAggregationInterval = fido2MetricsAggregationInterval;
	}

	public List<String> getPersonCustomObjectClassList() {
		return personCustomObjectClassList;
	}

	public void setPersonCustomObjectClassList(List<String> personCustomObjectClassList) {
		this.personCustomObjectClassList = personCustomObjectClassList;
	}

	public Fido2Configuration getFido2Configuration() {
		return fido2Configuration;
	}

	public void setFido2Configuration(Fido2Configuration fido2Configuration) {
		this.fido2Configuration = fido2Configuration;
	}

	



}

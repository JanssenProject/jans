/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.as.model.configuration.Configuration;

import io.jans.doc.annotation.DocProperty;
import jakarta.enterprise.inject.Vetoed;
/**
 * Represents the configuration JSON file.
 *
 * @author Yuriy Movchan
 * @version May 13, 2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class AppConfiguration implements Configuration {

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

	public Boolean getMetricReporterEnabled() {
		return metricReporterEnabled;
	}

	public void setMetricReporterEnabled(Boolean metricReporterEnabled) {
		this.metricReporterEnabled = metricReporterEnabled;
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

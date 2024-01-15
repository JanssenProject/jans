/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.model.config;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;
import jakarta.enterprise.inject.Vetoed;

/**
 * Janssen Project configuration
 *
 * @author Yuriy Movchan Date: 12/12/2023
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration {

	private String baseDN;

	private List<String> tokenChannels;

	@DocProperty(description = "Choose whether to disable JDK loggers", defaultValue = "true")
	private Boolean disableJdkLogger = true;

	private String loggingLevel;
	private String loggingLayout;
	private String externalLoggerConfiguration;

	private int metricReporterInterval;
	private int metricReporterKeepDataDays;
	private Boolean metricReporterEnabled;

	// Period in seconds
	private int cleanServiceInterval;
	
	private OpaConfiguration opaConfiguration;

	private String messageConsumerType;
	private String policyConsumerType;

	private String policiesJsonUrisAccessToken;
	private List<String> policiesJsonUris;

	private String policiesZipUrisAccessToken;
	private List<String> policiesZipUris;

	public String getBaseDN() {
		return baseDN;
	}

	public void setBaseDN(String baseDN) {
		this.baseDN = baseDN;
	}

	public List<String> getTokenChannels() {
		if (tokenChannels == null) {
			tokenChannels = new ArrayList<>();
		}

		return tokenChannels;
	}

	public void setTokenChannels(List<String> tokenChannels) {
		this.tokenChannels = tokenChannels;
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

	public int getCleanServiceInterval() {
		return cleanServiceInterval;
	}

	public void setCleanServiceInterval(int cleanServiceInterval) {
		this.cleanServiceInterval = cleanServiceInterval;
	}

	public OpaConfiguration getOpaConfiguration() {
		return opaConfiguration;
	}

	public void setOpaConfiguration(OpaConfiguration opaConfiguration) {
		this.opaConfiguration = opaConfiguration;
	}

	public String getMessageConsumerType() {
		return messageConsumerType;
	}

	public void setMessageConsumerType(String messageConsumerType) {
		this.messageConsumerType = messageConsumerType;
	}

	public String getPolicyConsumerType() {
		return policyConsumerType;
	}

	public void setPolicyConsumerType(String policyConsumerType) {
		this.policyConsumerType = policyConsumerType;
	}

	public String getPoliciesJsonUrisAccessToken() {
		return policiesJsonUrisAccessToken;
	}

	public void setPoliciesJsonUrisAccessToken(String policiesJsonUrisAccessToken) {
		this.policiesJsonUrisAccessToken = policiesJsonUrisAccessToken;
	}

	public List<String> getPoliciesJsonUris() {
		return policiesJsonUris;
	}

	public void setPoliciesJsonUris(List<String> policiesJsonUris) {
		this.policiesJsonUris = policiesJsonUris;
	}

	public String getPoliciesZipUrisAccessToken() {
		return policiesZipUrisAccessToken;
	}

	public void setPoliciesZipUrisAccessToken(String policiesZipUrisAccessToken) {
		this.policiesZipUrisAccessToken = policiesZipUrisAccessToken;
	}

	public List<String> getPoliciesZipUris() {
		return policiesZipUris;
	}

	public void setPoliciesZipUris(List<String> policiesZipUris) {
		this.policiesZipUris = policiesZipUris;
	}

}

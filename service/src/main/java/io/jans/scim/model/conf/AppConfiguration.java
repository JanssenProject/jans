package io.jans.scim.model.conf;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;

/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.config.oxtrust.Configuration;
import io.jans.config.oxtrust.ScimProperties;


/**
 * Janssen Project configuration
 *
 * @author Yuriy Movchan
 * @version 0.1, 05/15/2013
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration, Serializable {

    private static final long serialVersionUID = -8991383390239617013L;

    private String baseDN;

    private String applicationUrl;

    private String baseEndpoint;

    private String personCustomObjectClass;

    private String oxAuthIssuer;

    private String umaIssuer;

    private String scimUmaClientId;
    private String scimUmaClientKeyId;
    private String scimUmaResourceId;
    private String scimUmaScope;
    private String scimUmaClientKeyStoreFile;
    private String scimUmaClientKeyStorePassword;

    private boolean scimTestMode;

    private boolean rptConnectionPoolUseConnectionPooling;
    private int rptConnectionPoolMaxTotal;
    private int rptConnectionPoolDefaultMaxPerRoute;
    private int rptConnectionPoolValidateAfterInactivity; // In seconds; will be converted to millis
    private int rptConnectionPoolCustomKeepAliveTimeout; // In seconds; will be converted to millis


    private String loggingLevel;
    private String loggingLayout;
    private String externalLoggerConfiguration;

    private String organizationName;

    private int metricReporterInterval;
    private int metricReporterKeepDataDays;
    private Boolean metricReporterEnabled;
    private Boolean disableJdkLogger = true;

    @JsonProperty("ScimProperties")
    private ScimProperties scimProperties;

    private Boolean useLocalCache = false;

    public String getBaseDN() {
		return baseDN;
	}

	public void setBaseDN(String baseDN) {
		this.baseDN = baseDN;
	}

	public String getApplicationUrl() {
		return applicationUrl;
	}

	public void setApplicationUrl(String applicationUrl) {
		this.applicationUrl = applicationUrl;
	}

	public ScimProperties getScimProperties() {
        return scimProperties;
    }

    public void setScimProperties(ScimProperties scimProperties) {
        this.scimProperties = scimProperties;
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

    public String getOxAuthIssuer() {
        return oxAuthIssuer;
    }

    public void setOxAuthIssuer(String oxAuthIssuer) {
        this.oxAuthIssuer = oxAuthIssuer;
    }

    public String getUmaIssuer() {
        return umaIssuer;
    }

    public void setUmaIssuer(String umaIssuer) {
        this.umaIssuer = umaIssuer;
    }

    public String getScimUmaClientId() {
        return scimUmaClientId;
    }

    public void setScimUmaClientId(String scimUmaClientId) {
        this.scimUmaClientId = scimUmaClientId;
    }

    public String getScimUmaClientKeyId() {
        return scimUmaClientKeyId;
    }

    public void setScimUmaClientKeyId(String scimUmaClientKeyId) {
        this.scimUmaClientKeyId = scimUmaClientKeyId;
    }

    public String getScimUmaResourceId() {
        return scimUmaResourceId;
    }

    public void setScimUmaResourceId(String scimUmaResourceId) {
        this.scimUmaResourceId = scimUmaResourceId;
    }

    public String getScimUmaScope() {
        return scimUmaScope;
    }

    public void setScimUmaScope(String scimUmaScope) {
        this.scimUmaScope = scimUmaScope;
    }

    public String getScimUmaClientKeyStoreFile() {
        return scimUmaClientKeyStoreFile;
    }

    public void setScimUmaClientKeyStoreFile(String scimUmaClientKeyStoreFile) {
        this.scimUmaClientKeyStoreFile = scimUmaClientKeyStoreFile;
    }

    public String getScimUmaClientKeyStorePassword() {
        return scimUmaClientKeyStorePassword;
    }

    public void setScimUmaClientKeyStorePassword(String scimUmaClientKeyStorePassword) {
        this.scimUmaClientKeyStorePassword = scimUmaClientKeyStorePassword;
    }

    public boolean isScimTestMode() {
        return scimTestMode;
    }

    public void setScimTestMode(boolean scimTestMode) {
        this.scimTestMode = scimTestMode;
    }

	public boolean isRptConnectionPoolUseConnectionPooling() {
        return rptConnectionPoolUseConnectionPooling;
    }

    public void setRptConnectionPoolUseConnectionPooling(boolean rptConnectionPoolUseConnectionPooling) {
        this.rptConnectionPoolUseConnectionPooling = rptConnectionPoolUseConnectionPooling;
    }

    public int getRptConnectionPoolMaxTotal() {
        return rptConnectionPoolMaxTotal;
    }

    public void setRptConnectionPoolMaxTotal(int rptConnectionPoolMaxTotal) {
        this.rptConnectionPoolMaxTotal = rptConnectionPoolMaxTotal;
    }

    public int getRptConnectionPoolDefaultMaxPerRoute() {
        return rptConnectionPoolDefaultMaxPerRoute;
    }

    public void setRptConnectionPoolDefaultMaxPerRoute(int rptConnectionPoolDefaultMaxPerRoute) {
        this.rptConnectionPoolDefaultMaxPerRoute = rptConnectionPoolDefaultMaxPerRoute;
    }

    public int getRptConnectionPoolValidateAfterInactivity() {
        return rptConnectionPoolValidateAfterInactivity;
    }

    public void setRptConnectionPoolValidateAfterInactivity(int rptConnectionPoolValidateAfterInactivity) {
        this.rptConnectionPoolValidateAfterInactivity = rptConnectionPoolValidateAfterInactivity;
    }

    public int getRptConnectionPoolCustomKeepAliveTimeout() {
        return rptConnectionPoolCustomKeepAliveTimeout;
    }

    public void setRptConnectionPoolCustomKeepAliveTimeout(int rptConnectionPoolCustomKeepAliveTimeout) {
        this.rptConnectionPoolCustomKeepAliveTimeout = rptConnectionPoolCustomKeepAliveTimeout;
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


}

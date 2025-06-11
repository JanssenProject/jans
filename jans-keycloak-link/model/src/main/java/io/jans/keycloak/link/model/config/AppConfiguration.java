/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.keycloak.link.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.enterprise.inject.Vetoed;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Janssen Project configuration
 *
 * @author Yuriy Movchan
 * @version 0.1, 05/15/2013
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration extends LinkConfiguration {

    private String baseDN;

    private String[] personObjectClassTypes;
    private String personCustomObjectClass;

    private String[] personObjectClassDisplayNames;

    private String[] contactObjectClassTypes;

    private boolean allowPersonModification;

    // In seconds; will be converted to millis
    // In seconds; will be converted to millis

    private List<String> supportedUserStatus= Arrays.asList("active","inactive");

    private String loggingLevel;
    private String loggingLayout;
    private String externalLoggerConfiguration;

    private int metricReporterInterval;
    private int metricReporterKeepDataDays;
    private Boolean metricReporterEnabled;
    private Boolean disableJdkLogger = true;

    // in seconds
    private int cleanServiceInterval;

    private boolean keycloakLinkEnabled;
	private String keycloakLinkServerIpAddress;

    private String keycloakLinkPollingInterval;

    private Date keycloakLinkLastUpdate;
	private String keycloakLinkLastUpdateCount;
	private String keycloakLinkProblemCount;

    private Boolean useLocalCache = false;

    public String getBaseDN() {
        return baseDN;
    }

    public String[] getPersonObjectClassTypes() {
        return personObjectClassTypes;
    }

    public String getPersonCustomObjectClass() {
        return personCustomObjectClass;
    }

    public String[] getContactObjectClassTypes() {
        return contactObjectClassTypes;
    }


    public boolean isAllowPersonModification() {
        return allowPersonModification;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public String getLoggingLayout() {
        return loggingLayout;
    }

    public String getExternalLoggerConfiguration() {
		return externalLoggerConfiguration;
	}

    public int getMetricReporterInterval() {
        return metricReporterInterval;
    }

    public List<String> getSupportedUserStatus() {
        return supportedUserStatus;
    }

    public int getMetricReporterKeepDataDays() {
        return metricReporterKeepDataDays;
    }

    public Boolean getMetricReporterEnabled() {
        return metricReporterEnabled;
    }

    public Boolean getDisableJdkLogger() {
        return disableJdkLogger;
    }

    public int getCleanServiceInterval() {
        return cleanServiceInterval;
    }

    public Boolean getUseLocalCache() {
		return useLocalCache;
	}

    public boolean isKeycloakLinkEnabled() {
        return keycloakLinkEnabled;
    }

    public void setKeycloakLinkEnabled(boolean keycloakLinkEnabled) {
        this.keycloakLinkEnabled = keycloakLinkEnabled;
    }

    public String getKeycloakLinkServerIpAddress() {
        return keycloakLinkServerIpAddress;
    }

    public void setKeycloakLinkServerIpAddress(String keycloakLinkServerIpAddress) {
        this.keycloakLinkServerIpAddress = keycloakLinkServerIpAddress;
    }

    public String getKeycloakLinkPollingInterval() {
        return keycloakLinkPollingInterval;
    }

    public void setKeycloakLinkPollingInterval(String keycloakLinkPollingInterval) {
        this.keycloakLinkPollingInterval = keycloakLinkPollingInterval;
    }

    public Date getKeycloakLinkLastUpdate() {
        return keycloakLinkLastUpdate;
    }

    public void setKeycloakLinkLastUpdate(Date keycloakLinkLastUpdate) {
        this.keycloakLinkLastUpdate = keycloakLinkLastUpdate;
    }

    public String getKeycloakLinkLastUpdateCount() {
        return keycloakLinkLastUpdateCount;
    }

    public void setKeycloakLinkLastUpdateCount(String keycloakLinkLastUpdateCount) {
        this.keycloakLinkLastUpdateCount = keycloakLinkLastUpdateCount;
    }

    public String getKeycloakLinkProblemCount() {
        return keycloakLinkProblemCount;
    }

    public void setKeycloakLinkProblemCount(String keycloakLinkProblemCount) {
        this.keycloakLinkProblemCount = keycloakLinkProblemCount;
    }
}

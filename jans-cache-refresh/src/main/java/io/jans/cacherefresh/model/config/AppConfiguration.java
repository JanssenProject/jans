/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.cacherefresh.model.config;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.enterprise.inject.Vetoed;

/**
 * Janssen Project configuration
 *
 * @author Yuriy Movchan
 * @version 0.1, 05/15/2013
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration extends CacheRefreshConfiguration {

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

    private boolean vdsCacheRefreshEnabled;
	private String cacheRefreshServerIpAddress;
	private String vdsCacheRefreshPollingInterval;
	private Date vdsCacheRefreshLastUpdate;
	private String vdsCacheRefreshLastUpdateCount;
	private String vdsCacheRefreshProblemCount;

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

    public boolean isVdsCacheRefreshEnabled() {
		return vdsCacheRefreshEnabled;
	}

	public void setVdsCacheRefreshEnabled(boolean vdsCacheRefreshEnabled) {
		this.vdsCacheRefreshEnabled = vdsCacheRefreshEnabled;
	}

	public String getCacheRefreshServerIpAddress() {
		return cacheRefreshServerIpAddress;
	}

	public void setCacheRefreshServerIpAddress(String cacheRefreshServerIpAddress) {
		this.cacheRefreshServerIpAddress = cacheRefreshServerIpAddress;
	}

	public String getVdsCacheRefreshPollingInterval() {
		return vdsCacheRefreshPollingInterval;
	}

    public void setVdsCacheRefreshLastUpdate(Date vdsCacheRefreshLastUpdate) {
		this.vdsCacheRefreshLastUpdate = vdsCacheRefreshLastUpdate;
	}

	public String getVdsCacheRefreshLastUpdateCount() {
		return vdsCacheRefreshLastUpdateCount;
	}

	public void setVdsCacheRefreshLastUpdateCount(String vdsCacheRefreshLastUpdateCount) {
		this.vdsCacheRefreshLastUpdateCount = vdsCacheRefreshLastUpdateCount;
	}

	public String getVdsCacheRefreshProblemCount() {
		return vdsCacheRefreshProblemCount;
	}

	public void setVdsCacheRefreshProblemCount(String vdsCacheRefreshProblemCount) {
		this.vdsCacheRefreshProblemCount = vdsCacheRefreshProblemCount;
	}

}

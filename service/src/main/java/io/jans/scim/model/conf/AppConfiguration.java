package io.jans.scim.model.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.config.oxtrust.Configuration;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;


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
    
    private int maxCount;
    private String userExtensionSchemaURI;

    private String loggingLevel;
    private String loggingLayout;
    private String externalLoggerConfiguration;

    private int metricReporterInterval;
    private int metricReporterKeepDataDays;
    private Boolean metricReporterEnabled;
    private Boolean disableJdkLogger = true;

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

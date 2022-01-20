package io.jans.configapi.plugin.scim.model.config;

import io.jans.config.oxtrust.Configuration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration,Serializable {

    private static final long serialVersionUID = -1832522737483366789L;

    private String baseDN;

    private String applicationUrl;

    private String baseEndpoint;

    private String personCustomObjectClass;

    private String oxAuthIssuer;
    
    private String protectionMode;
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

    @Override
    public String toString() {
        return "AppConfiguration [baseDN=" + baseDN + ", applicationUrl=" + applicationUrl + ", baseEndpoint="
                + baseEndpoint + ", personCustomObjectClass=" + personCustomObjectClass + ", oxAuthIssuer="
                + oxAuthIssuer + ", protectionMode=" + protectionMode + ", maxCount=" + maxCount
                + ", userExtensionSchemaURI=" + userExtensionSchemaURI + ", loggingLevel=" + loggingLevel
                + ", loggingLayout=" + loggingLayout + ", externalLoggerConfiguration=" + externalLoggerConfiguration
                + ", metricReporterInterval=" + metricReporterInterval + ", metricReporterKeepDataDays="
                + metricReporterKeepDataDays + ", metricReporterEnabled=" + metricReporterEnabled
                + ", disableJdkLogger=" + disableJdkLogger + ", useLocalCache=" + useLocalCache + "]";
    }
	
	

}

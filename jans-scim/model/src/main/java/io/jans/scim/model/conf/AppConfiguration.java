package io.jans.scim.model.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.doc.annotation.DocProperty;

import java.io.Serializable;

/**
 * Janssen Project configuration
 *
 * @author Yuriy Movchan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration, Serializable {

    private static final long serialVersionUID = -8991383390239617013L;

    @DocProperty(description = "Application config Base DN")
    private String baseDN;
    @DocProperty(description = "Application base URL")
    private String applicationUrl;
    @DocProperty(description = "SCIM base endpoint URL")
    private String baseEndpoint;
    @DocProperty(description = "Person Object Class")
    private String personCustomObjectClass;
    @DocProperty(description = "Jans Auth - Issuer identifier")
    private String oxAuthIssuer;
    @DocProperty(description = "SCIM Protection Mode")
    private ScimMode protectionMode;
    @DocProperty(description = "Maximum number of results per page")
    private int maxCount;
    @DocProperty(description = "Specifies maximum bulk operations")
    private int bulkMaxOperations;
    @DocProperty(description = "Specifies maximum payload size of bulk operations")
    private long bulkMaxPayloadSize;
    @DocProperty(description = "User Extension Schema URI")
    private String userExtensionSchemaURI;
    @DocProperty(description = "Logging level for scim logger")
    private String loggingLevel;
    @DocProperty(description = "Logging layout used for Server loggers")
    private String loggingLayout;
    @DocProperty(description = "Path to external log4j2 logging configuration")
    private String externalLoggerConfiguration;
    @DocProperty(description = "The interval for metric reporter in seconds")
    private int metricReporterInterval;
    @DocProperty(description = "The days to keep metric reported data")
    private int metricReporterKeepDataDays;
    @DocProperty(description = "Metric reported data enabled flag")
    private Boolean metricReporterEnabled;
    @DocProperty(description = "Boolean value specifying whether to enable JDK Loggers")
    private Boolean disableJdkLogger = true;
    @DocProperty(description = "Boolean value specifying whether to enable local in-memory cache")
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

    public ScimMode getProtectionMode() {
        return protectionMode;
    }

    public void setProtectionMode(ScimMode protectionMode) {
        this.protectionMode = protectionMode;
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
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

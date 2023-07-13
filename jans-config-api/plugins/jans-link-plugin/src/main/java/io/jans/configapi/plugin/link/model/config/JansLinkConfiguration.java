package io.jans.configapi.plugin.link.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.config.oxtrust.CacheRefreshAttributeMapping;
import io.jans.config.oxtrust.Configuration;
import io.jans.model.ldap.GluuLdapConfiguration;
import jakarta.enterprise.inject.Vetoed;

import java.util.Arrays;
import java.util.List;
import java.util.Date;

@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class JansLinkConfiguration implements Configuration {

    private List<GluuLdapConfiguration> sourceConfigs;
    private GluuLdapConfiguration inumConfig;
    private GluuLdapConfiguration targetConfig;

    private int ldapSearchSizeLimit;

    private List<String> keyAttributes;
    private List<String> keyObjectClasses;
    private List<String> sourceAttributes;

    private String customLdapFilter;

    private String updateMethod;

    private boolean defaultInumServer;

    private boolean keepExternalPerson;

    private boolean useSearchLimit;

    private List<CacheRefreshAttributeMapping> attributeMapping;

    private String snapshotFolder;
    private int snapshotMaxCount;
    
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
    private boolean linkEnabled;
    private String serverIpAddress;
    private String pollingInterval;

    private Date lastUpdate;
    private String lastUpdateCount;
    private String problemCount;

    private Boolean useLocalCache = false;
    
    
    public boolean isLinkEnabled() {
        return linkEnabled;
    }

    public void setLinkEnabled(boolean linkEnabled) {
        this.linkEnabled = linkEnabled;
    }

    public String getServerIpAddress() {
        return serverIpAddress;
    }

    public void setServerIpAddress(String serverIpAddress) {
        this.serverIpAddress = serverIpAddress;
    }

    public String getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(String pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getLastUpdateCount() {
        return lastUpdateCount;
    }

    public void setLastUpdateCount(String lastUpdateCount) {
        this.lastUpdateCount = lastUpdateCount;
    }

    public String getProblemCount() {
        return problemCount;
    }

    public void setProblemCount(String problemCount) {
        this.problemCount = problemCount;
    }

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
    
    public List<GluuLdapConfiguration> getSourceConfigs() {
        return sourceConfigs;
    }

    public void setSourceConfigs(List<GluuLdapConfiguration> sourceConfigs) {
        this.sourceConfigs = sourceConfigs;
    }

    public GluuLdapConfiguration getInumConfig() {
        return inumConfig;
    }

    public void setInumConfig(GluuLdapConfiguration inumConfig) {
        this.inumConfig = inumConfig;
    }

    public GluuLdapConfiguration getTargetConfig() {
        return targetConfig;
    }

    public void setTargetConfig(GluuLdapConfiguration targetConfig) {
        this.targetConfig = targetConfig;
    }

    public int getLdapSearchSizeLimit() {
        return ldapSearchSizeLimit;
    }

    public void setLdapSearchSizeLimit(int ldapSearchSizeLimit) {
        this.ldapSearchSizeLimit = ldapSearchSizeLimit;
    }

    public List<String> getKeyAttributes() {
        return keyAttributes;
    }

    public void setKeyAttributes(List<String> keyAttributes) {
        this.keyAttributes = keyAttributes;
    }

    public List<String> getKeyObjectClasses() {
        return keyObjectClasses;
    }

    public void setKeyObjectClasses(List<String> keyObjectClasses) {
        this.keyObjectClasses = keyObjectClasses;
    }

    public List<String> getSourceAttributes() {
        return sourceAttributes;
    }

    public void setSourceAttributes(List<String> sourceAttributes) {
        this.sourceAttributes = sourceAttributes;
    }

    public String getCustomLdapFilter() {
        return customLdapFilter;
    }

    public void setCustomLdapFilter(String customLdapFilter) {
        this.customLdapFilter = customLdapFilter;
    }

    public String getUpdateMethod() {
        return updateMethod;
    }

    public void setUpdateMethod(String updateMethod) {
        this.updateMethod = updateMethod;
    }

    public boolean isKeepExternalPerson() {
        return keepExternalPerson;
    }

    public void setKeepExternalPerson(boolean keepExternalPerson) {
        this.keepExternalPerson = keepExternalPerson;
    }

    public boolean isDefaultInumServer() {
        return defaultInumServer;
    }

    public void setDefaultInumServer(boolean defaultInumServer) {
        this.defaultInumServer = defaultInumServer;
    }

    public boolean isUseSearchLimit() {
        return useSearchLimit;
    }

    public void setUseSearchLimit(boolean useSearchLimit) {
        this.useSearchLimit = useSearchLimit;
    }

    public List<CacheRefreshAttributeMapping> getAttributeMapping() {
        return attributeMapping;
    }

    public void setAttributeMapping(List<CacheRefreshAttributeMapping> attributeMapping) {
        this.attributeMapping = attributeMapping;
    }

    public String getSnapshotFolder() {
        return snapshotFolder;
    }

    public void setSnapshotFolder(String snapshotFolder) {
        this.snapshotFolder = snapshotFolder;
    }

    public int getSnapshotMaxCount() {
        return snapshotMaxCount;
    }

    public void setSnapshotMaxCount(int snapshotMaxCount) {
        this.snapshotMaxCount = snapshotMaxCount;
    }

}

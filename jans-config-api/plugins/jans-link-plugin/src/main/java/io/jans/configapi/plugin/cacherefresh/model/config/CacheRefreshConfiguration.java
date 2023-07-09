package io.jans.configapi.plugin.cacherefresh.model.config;

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
public class CacheRefreshConfiguration implements Configuration {

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

    private boolean vdsCacheRefreshEnabled;
    private String cacheRefreshServerIpAddress;
    private String vdsCacheRefreshPollingInterval;
    private Date vdsCacheRefreshLastUpdate;
    private String vdsCacheRefreshLastUpdateCount;
    private String vdsCacheRefreshProblemCount;

    private Boolean useLocalCache = false;

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

    public boolean isDefaultInumServer() {
        return defaultInumServer;
    }

    public void setDefaultInumServer(boolean defaultInumServer) {
        this.defaultInumServer = defaultInumServer;
    }

    public boolean isKeepExternalPerson() {
        return keepExternalPerson;
    }

    public void setKeepExternalPerson(boolean keepExternalPerson) {
        this.keepExternalPerson = keepExternalPerson;
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

    public String getBaseDN() {
        return baseDN;
    }

    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }

    public String[] getPersonObjectClassTypes() {
        return personObjectClassTypes;
    }

    public void setPersonObjectClassTypes(String[] personObjectClassTypes) {
        this.personObjectClassTypes = personObjectClassTypes;
    }

    public String getPersonCustomObjectClass() {
        return personCustomObjectClass;
    }

    public void setPersonCustomObjectClass(String personCustomObjectClass) {
        this.personCustomObjectClass = personCustomObjectClass;
    }

    public String[] getPersonObjectClassDisplayNames() {
        return personObjectClassDisplayNames;
    }

    public void setPersonObjectClassDisplayNames(String[] personObjectClassDisplayNames) {
        this.personObjectClassDisplayNames = personObjectClassDisplayNames;
    }

    public String[] getContactObjectClassTypes() {
        return contactObjectClassTypes;
    }

    public void setContactObjectClassTypes(String[] contactObjectClassTypes) {
        this.contactObjectClassTypes = contactObjectClassTypes;
    }

    public boolean isAllowPersonModification() {
        return allowPersonModification;
    }

    public void setAllowPersonModification(boolean allowPersonModification) {
        this.allowPersonModification = allowPersonModification;
    }

    public List<String> getSupportedUserStatus() {
        return supportedUserStatus;
    }

    public void setSupportedUserStatus(List<String> supportedUserStatus) {
        this.supportedUserStatus = supportedUserStatus;
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

    public int getCleanServiceInterval() {
        return cleanServiceInterval;
    }

    public void setCleanServiceInterval(int cleanServiceInterval) {
        this.cleanServiceInterval = cleanServiceInterval;
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

    public void setVdsCacheRefreshPollingInterval(String vdsCacheRefreshPollingInterval) {
        this.vdsCacheRefreshPollingInterval = vdsCacheRefreshPollingInterval;
    }

    public Date getVdsCacheRefreshLastUpdate() {
        return vdsCacheRefreshLastUpdate;
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

    public Boolean getUseLocalCache() {
        return useLocalCache;
    }

    public void setUseLocalCache(Boolean useLocalCache) {
        this.useLocalCache = useLocalCache;
    }

}

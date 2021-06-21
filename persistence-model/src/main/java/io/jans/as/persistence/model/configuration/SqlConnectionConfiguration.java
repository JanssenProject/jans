package io.jans.as.persistence.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SqlConnectionConfiguration {
    private String configId;
    private String userName;
    private String userPassword;
    private List<String> connectionUri;
    private String schemaName;
    private String serverTimezone;
    private int useServerPrepStmts;
    private int prepStmtCacheSqlLimit;
    private Boolean cachePrepStmts;
    private Boolean cacheResultSetMetadata;
    private int metadataCacheSize;
    private String passwordEncryptionMethod;
    private int connectionPoolMaxTotal;
    private int connectionPoolMaxIdle;
    private int connectionPoolMinIdle;
    private int createMaxWaitTimeMillis;
    private int maxWaitTimeMillis;
    private int minEvictableIdleTimeMillis;
    private List<String> binaryAttributes;
    private List<String> certificateAttributes;

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public List<String> getConnectionUri() {
        return connectionUri;
    }

    public void setConnectionUri(List<String> connectionUri) {
        this.connectionUri = connectionUri;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getServerTimezone() {
        return serverTimezone;
    }

    public void setServerTimezone(String serverTimezone) {
        this.serverTimezone = serverTimezone;
    }

    public int getUseServerPrepStmts() {
        return useServerPrepStmts;
    }

    public void setUseServerPrepStmts(int useServerPrepStmts) {
        this.useServerPrepStmts = useServerPrepStmts;
    }

    public int getPrepStmtCacheSqlLimit() {
        return prepStmtCacheSqlLimit;
    }

    public void setPrepStmtCacheSqlLimit(int prepStmtCacheSqlLimit) {
        this.prepStmtCacheSqlLimit = prepStmtCacheSqlLimit;
    }

    public Boolean getCachePrepStmts() {
        return cachePrepStmts;
    }

    public void setCachePrepStmts(Boolean cachePrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
    }

    public Boolean getCacheResultSetMetadata() {
        return cacheResultSetMetadata;
    }

    public void setCacheResultSetMetadata(Boolean cacheResultSetMetadata) {
        this.cacheResultSetMetadata = cacheResultSetMetadata;
    }

    public int getMetadataCacheSize() {
        return metadataCacheSize;
    }

    public void setMetadataCacheSize(int metadataCacheSize) {
        this.metadataCacheSize = metadataCacheSize;
    }

    public String getPasswordEncryptionMethod() {
        return passwordEncryptionMethod;
    }

    public void setPasswordEncryptionMethod(String passwordEncryptionMethod) {
        this.passwordEncryptionMethod = passwordEncryptionMethod;
    }

    public int getConnectionPoolMaxTotal() {
        return connectionPoolMaxTotal;
    }

    public void setConnectionPoolMaxTotal(int connectionPoolMaxTotal) {
        this.connectionPoolMaxTotal = connectionPoolMaxTotal;
    }

    public int getConnectionPoolMaxIdle() {
        return connectionPoolMaxIdle;
    }

    public void setConnectionPoolMaxIdle(int connectionPoolMaxIdle) {
        this.connectionPoolMaxIdle = connectionPoolMaxIdle;
    }

    public int getConnectionPoolMinIdle() {
        return connectionPoolMinIdle;
    }

    public void setConnectionPoolMinIdle(int connectionPoolMinIdle) {
        this.connectionPoolMinIdle = connectionPoolMinIdle;
    }

    public int getCreateMaxWaitTimeMillis() {
        return createMaxWaitTimeMillis;
    }

    public void setCreateMaxWaitTimeMillis(int createMaxWaitTimeMillis) {
        this.createMaxWaitTimeMillis = createMaxWaitTimeMillis;
    }

    public int getMaxWaitTimeMillis() {
        return maxWaitTimeMillis;
    }

    public void setMaxWaitTimeMillis(int maxWaitTimeMillis) {
        this.maxWaitTimeMillis = maxWaitTimeMillis;
    }

    public int getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public List<String> getBinaryAttributes() {
        return binaryAttributes;
    }

    public void setBinaryAttributes(List<String> binaryAttributes) {
        this.binaryAttributes = binaryAttributes;
    }

    public List<String> getCertificateAttributes() {
        return certificateAttributes;
    }

    public void setCertificateAttributes(List<String> certificateAttributes) {
        this.certificateAttributes = certificateAttributes;
    }
}

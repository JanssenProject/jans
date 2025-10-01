package io.jans.orm.sql.model;

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
    private Integer useServerPrepStmts;
    private Integer prepStmtCacheSqlLimit;
    private Boolean cachePrepStmts;
    private Boolean cacheResultSetMetadata;
    private Integer metadataCacheSize;
    private String passwordEncryptionMethod;
    private Integer connectionPoolMaxTotal;
    private Integer connectionPoolMaxIdle;
    private Integer connectionPoolMinIdle;
    private Integer createMaxWaitTimeMillis;
    private Integer maxWaitTimeMillis;
    private Integer minEvictableIdleTimeMillis;
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

    public Integer getUseServerPrepStmts() {
        return useServerPrepStmts;
    }

    public void setUseServerPrepStmts(Integer useServerPrepStmts) {
        this.useServerPrepStmts = useServerPrepStmts;
    }

    public Integer getPrepStmtCacheSqlLimit() {
        return prepStmtCacheSqlLimit;
    }

    public void setPrepStmtCacheSqlLimit(Integer prepStmtCacheSqlLimit) {
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

    public Integer getMetadataCacheSize() {
        return metadataCacheSize;
    }

    public void setMetadataCacheSize(Integer metadataCacheSize) {
        this.metadataCacheSize = metadataCacheSize;
    }

    public String getPasswordEncryptionMethod() {
        return passwordEncryptionMethod;
    }

    public void setPasswordEncryptionMethod(String passwordEncryptionMethod) {
        this.passwordEncryptionMethod = passwordEncryptionMethod;
    }

    public Integer getConnectionPoolMaxTotal() {
        return connectionPoolMaxTotal;
    }

    public void setConnectionPoolMaxTotal(Integer connectionPoolMaxTotal) {
        this.connectionPoolMaxTotal = connectionPoolMaxTotal;
    }

    public Integer getConnectionPoolMaxIdle() {
        return connectionPoolMaxIdle;
    }

    public void setConnectionPoolMaxIdle(Integer connectionPoolMaxIdle) {
        this.connectionPoolMaxIdle = connectionPoolMaxIdle;
    }

    public Integer getConnectionPoolMinIdle() {
        return connectionPoolMinIdle;
    }

    public void setConnectionPoolMinIdle(Integer connectionPoolMinIdle) {
        this.connectionPoolMinIdle = connectionPoolMinIdle;
    }

    public Integer getCreateMaxWaitTimeMillis() {
        return createMaxWaitTimeMillis;
    }

    public void setCreateMaxWaitTimeMillis(Integer createMaxWaitTimeMillis) {
        this.createMaxWaitTimeMillis = createMaxWaitTimeMillis;
    }

    public Integer getMaxWaitTimeMillis() {
        return maxWaitTimeMillis;
    }

    public void setMaxWaitTimeMillis(Integer maxWaitTimeMillis) {
        this.maxWaitTimeMillis = maxWaitTimeMillis;
    }

    public Integer getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(Integer minEvictableIdleTimeMillis) {
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

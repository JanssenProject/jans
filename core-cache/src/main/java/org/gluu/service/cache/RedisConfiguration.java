package org.gluu.service.cache;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * @author yuriyz on 02/23/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedisConfiguration implements Serializable {

    private static final long serialVersionUID = 5513197227832695471L;

    private RedisProviderType redisProviderType = RedisProviderType.STANDALONE;

    private String servers = "localhost:6379"; // server1:11211 server2:11211

    private int defaultPutExpiration = 60; // in seconds

    private String sentinelMasterGroupName = "";

    private String password;

    private String decryptedPassword;

    private Boolean useSSL = false;

    private String sslTrustStoreFilePath = "";

    /**
     * The cap on the number of "idle" instances in the pool. If maxIdle
     * is set too low on heavily loaded systems it is possible you will see
     * objects being destroyed and almost immediately new objects being created.
     * This is a result of the active threads momentarily returning objects
     * faster than they are requesting them, causing the number of idle
     * objects to rise above maxIdle. The best value for maxIdle for heavily
     * loaded system will vary but the default is a good starting point.
     */
    private int maxIdleConnections = 10;

    private int maxTotalConnections = 10000;

    private int connectionTimeout = 3000;

    /**
     * With this option set to a non-zero timeout, a read() call on the InputStream associated with this Socket will block for only this amount of time.
     * If the timeout expires, a java.net.SocketTimeoutException is raised, though the Socket is still valid.
     * The option must be enabled prior to entering the blocking operation to have effect. The timeout must be > 0.
     * A timeout of zero is interpreted as an infinite timeout.
     */
    private int soTimeout = 3000;

    private int maxRetryAttempts = 5;

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public int getDefaultPutExpiration() {
        return defaultPutExpiration;
    }

    public void setDefaultPutExpiration(int defaultPutExpiration) {
        this.defaultPutExpiration = defaultPutExpiration;
    }

    public RedisProviderType getRedisProviderType() {
        return redisProviderType;
    }

    public void setRedisProviderType(RedisProviderType redisProviderType) {
        this.redisProviderType = redisProviderType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getUseSSL() {
        return useSSL != null ? useSSL : false;
    }

    public void setUseSSL(Boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getSslTrustStoreFilePath() {
        return sslTrustStoreFilePath;
    }

    public void setSslTrustStoreFilePath(String sslTrustStoreFilePath) {
        this.sslTrustStoreFilePath = sslTrustStoreFilePath;
    }

    public String getDecryptedPassword() {
        return decryptedPassword;
    }

    public void setDecryptedPassword(String decryptedPassword) {
        this.decryptedPassword = decryptedPassword;
    }

    public String getSentinelMasterGroupName() {
        return sentinelMasterGroupName;
    }

    public void setSentinelMasterGroupName(String sentinelMasterGroupName) {
        this.sentinelMasterGroupName = sentinelMasterGroupName;
    }

    @Override
    public String toString() {
        return "RedisConfiguration{" +
                "servers='" + servers + '\'' +
                ", defaultPutExpiration=" + defaultPutExpiration +
                ", redisProviderType=" + redisProviderType +
                ", useSSL=" + useSSL +
                ", sslTrustStoreFilePath=" + sslTrustStoreFilePath +
                ", sentinelMasterGroupName=" + sentinelMasterGroupName +
                ", maxIdleConnections=" + maxIdleConnections +
                ", maxTotalConnections=" + maxTotalConnections +
                ", connectionTimeout=" + connectionTimeout +
                ", soTimeout=" + soTimeout +
                ", maxRetryAttempts=" + maxRetryAttempts +
                '}';
    }
}

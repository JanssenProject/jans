package org.xdi.service.cache;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

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

    private String password;

    private String decryptedPassword;

    private Boolean useSSL = false;

    private String sslTrustStoreFilePath = "";

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

    @Override
    public String toString() {
        return "RedisConfiguration{" +
                "servers='" + servers + '\'' +
                ", defaultPutExpiration=" + defaultPutExpiration +
                ", redisProviderType=" + redisProviderType +
                ", useSSL=" + useSSL +
                ", sslTrustStoreFilePath=" + sslTrustStoreFilePath +
                '}';
    }
}

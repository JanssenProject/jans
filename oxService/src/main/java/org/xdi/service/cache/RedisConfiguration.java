package org.xdi.service.cache;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

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

    @Override
    public String toString() {
        return "RedisConfiguration{" + "servers='" + servers + '\'' + ", defaultPutExpiration=" + defaultPutExpiration + ", redisProviderType="
                + redisProviderType + '}';
    }
}

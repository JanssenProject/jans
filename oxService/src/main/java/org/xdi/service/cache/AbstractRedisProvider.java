package org.xdi.service.cache;

/**
 * @author yuriyz
 */
public abstract class AbstractRedisProvider {

    protected RedisConfiguration redisConfiguration;

    public AbstractRedisProvider(RedisConfiguration redisConfiguration) {
        this.redisConfiguration = redisConfiguration;
    }

    public RedisConfiguration getRedisConfiguration() {
        return redisConfiguration;
    }

    public void testConnection() {
        put(2, "testKey", "testValue");
        if (!"testValue".equals(get("testKey"))) {
            throw new RuntimeException("Failed to connect to redis server. Configuration: " + redisConfiguration);
        }
    }

    public abstract void create();

    public abstract void destroy();

    public abstract Object getDelegate();

    public abstract Object get(String key);

    public abstract void remove(String key);

    public abstract void clear();

    public abstract void put(int expirationInSeconds, String key, Object object);

    public abstract void put(String key, Object object);

}

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

    abstract void create();

    abstract void destroy();

    abstract Object getDelegate();

    abstract Object get(String key);

    abstract void remove(String key);

    abstract void clear();

    abstract void put(int expirationInSeconds, String key, Object object);

    abstract void put(String key, Object object);

}

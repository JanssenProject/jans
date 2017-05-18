package org.xdi.service.cache;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author yuriyz on 02/23/2017.
 */
public class RedisProvider extends AbstractCacheProvider<JedisPool> {

	@Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    private JedisPool pool;
    private RedisConfiguration redisConfiguration;

    public RedisProvider() {}

    @PostConstruct
    public void init() {
    	this.redisConfiguration = cacheConfiguration.getRedisConfiguration();
    }

    public void create() {
        log.debug("Starting RedisProvider ...");

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(1000);
            poolConfig.setMinIdle(2);

            pool = new JedisPool(poolConfig, redisConfiguration.getHost(), redisConfiguration.getPort());

            testConnection();
            log.debug("RedisProvider started.");
        } catch (Exception e) {
            throw new IllegalStateException("Error starting RedisProvider", e);
        }
    }

    private void testConnection() {
        put("testKey", "testValue");
        if (!"testValue".equals(get("testKey"))) {
            throw new RuntimeException("Failed to connect to redis server. Configuration: " + redisConfiguration);
        }
    }

    @PreDestroy
    public void destroy() {
        log.debug("Destroying RedisProvider");

        pool.close();

        log.debug("Destroyed RedisProvider");
    }

    @Override
    public JedisPool getDelegate() {
        return pool;
    }

    @Override
    public Object get(String region, String key) {
        Jedis jedis = pool.getResource();
        try {
            byte[] value = jedis.get(key.getBytes());
            Object deserialized = null;
            if (value != null && value.length > 0) {
                deserialized = SerializationUtils.deserialize(value);
            }
            return deserialized;
        } finally {
            jedis.close();
        }
    }

    @Override // it is so weird but we use as workaround "region" field to pass "expiration" for put operation
    public void put(String expirationInSeconds, String key, Object object) {
        Jedis jedis = pool.getResource();
        try {
            String status = jedis.setex(key.getBytes(),
                    putExpiration(expirationInSeconds),
                    SerializationUtils.serialize((Serializable) object));
            log.trace("put - key: " + key + ", status: " + status);
        } finally {
            jedis.close();
        }
    }

    private int putExpiration(String expirationInSeconds) {
        try {
            return Integer.parseInt(expirationInSeconds);
        } catch (Exception e) {
            return redisConfiguration.getDefaultPutExpiration();
        }
    }

    @Override
    public void remove(String region, String key) {
        Jedis jedis = pool.getResource();
        try {
            Long entriesRemoved = jedis.del(key.getBytes());
            log.trace("remove - key: " + key + ", entriesRemoved: " + entriesRemoved);
        } finally {
            jedis.close();
        }
    }

    @Override
    public void clear() {
        Jedis jedis = pool.getResource();
        try {
            jedis.flushAll();
            log.trace("clear");
        } finally {
            jedis.close();
        }
    }

//    public static void main(String[] args) throws InterruptedException {
//        RedisConfiguration config = new RedisConfiguration();
//
//        RedisProvider cache = null;
//        try {
//            cache = new RedisProvider(config);
//            cache.create();
//
//            cache.put(Integer.toString(3), "myKey", CacheProviderType.IN_MEMORY);
//
//            System.out.println(cache.get("myKey"));
//
//            Thread.sleep(1000);
//
//            System.out.println(cache.get("myKey"));
//
//            Thread.sleep(3000);
//
//            System.out.println(cache.get("myKey"));
//
//        } finally {
//            cache.destroy();
//        }
//    }
}

package org.xdi.service.cache;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yuriyz on 02/23/2017.
 */
public class RedisProvider extends AbstractCacheProvider<ShardedJedisPool> {

	@Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    private ShardedJedisPool pool;
    private RedisConfiguration redisConfiguration;

    public RedisProvider() {}

    @PostConstruct
    public void init() {
    	this.redisConfiguration = cacheConfiguration.getRedisConfiguration();
    }

    public void create() {
        try {
            log.debug("Starting RedisProvider ... configuration:" + redisConfiguration);

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(1000);
            poolConfig.setMinIdle(2);

            pool = new ShardedJedisPool(poolConfig, shards(redisConfiguration.getServers()));

            testConnection();
            log.debug("RedisProvider started.");
        } catch (Exception e) {
            log.error("Failed to start RedisProvider.");
            throw new IllegalStateException("Error starting RedisProvider", e);
        }
    }

    private List<JedisShardInfo> shards(String servers) {
        final String[] serverWithPorts = StringUtils.split(servers.trim(), ",");

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        for (String serverWithPort : serverWithPorts) {
            final String[] split = serverWithPort.trim().split(":");
            String host = split[0];
            int port = Integer.parseInt(split[1].trim());
            shards.add(new JedisShardInfo(host, port));
        }
        return shards;
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
    public ShardedJedisPool getDelegate() {
        return pool;
    }

    @Override
    public Object get(String region, String key) {
        ShardedJedis jedis = pool.getResource();
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
        ShardedJedis jedis = pool.getResource();
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
        ShardedJedis jedis = pool.getResource();
        try {
            Long entriesRemoved = jedis.del(key.getBytes());
            log.trace("remove - key: " + key + ", entriesRemoved: " + entriesRemoved);
        } finally {
            jedis.close();
        }
    }

    @Override
    public void clear() {
//        ShardedJedis jedis = pool.getResource();
//        try {
            //jedis.flushAll();
            log.trace("clear not implemented");
//        } finally {
//            jedis.close();
//        }
    }

//    public static void main(String[] args) throws InterruptedException {
//        RedisConfiguration config = new RedisConfiguration();
//        config.setServers("localhost:6379 localhost:6379");
//
//        RedisProvider cache = null;
//        try {
//            cache = new RedisProvider();
//            cache.log = NOPLogger.NOP_LOGGER;
//            cache.redisConfiguration = config;
//            cache.create();
//
//            cache.put(Integer.toString(3), "myKey", "valueInRedis");
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

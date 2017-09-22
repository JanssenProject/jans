package org.xdi.service.cache;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Important : keep it weld free. It's reused by oxd !
 *
 * @author yuriyz on 02/23/2017.
 */
public class RedisShardedProvider extends AbstractRedisProvider {

    private final static Logger LOG = LoggerFactory.getLogger(RedisShardedProvider.class);

    private ShardedJedisPool pool;

    public RedisShardedProvider(RedisConfiguration redisConfiguration) {
        super(redisConfiguration);
    }

    public void create() {
        try {
            LOG.debug("Starting RedisShardedProvider ... configuration:" + redisConfiguration);

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(1000);
            poolConfig.setMinIdle(2);

            pool = new ShardedJedisPool(poolConfig, shards(redisConfiguration.getServers()));

            testConnection();
            LOG.debug("RedisShardedProvider started.");
        } catch (Exception e) {
            LOG.error("Failed to start RedisShardedProvider.");
            throw new IllegalStateException("Error starting RedisShardedProvider", e);
        }
    }

    private static List<JedisShardInfo> shards(String servers) {
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

    public void destroy() {
        LOG.debug("Destroying RedisShardedProvider");

        pool.close();

        LOG.debug("Destroyed RedisShardedProvider");
    }

    @Override
    public ShardedJedisPool getDelegate() {
        return pool;
    }

    @Override
    public Object get(String key) {
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

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        ShardedJedis jedis = pool.getResource();
        try {
            String status = jedis.setex(key.getBytes(),
                    expirationInSeconds,
                    SerializationUtils.serialize((Serializable) object));
            LOG.trace("put - key: " + key + ", status: " + status);
        } finally {
            jedis.close();
        }
    }

    @Override
    public void put(String key, Object object) {
        ShardedJedis jedis = pool.getResource();
        try {
            String status = jedis.set(key.getBytes(), SerializationUtils.serialize((Serializable) object));
            LOG.trace("put - key: " + key + ", status: " + status);
        } finally {
            jedis.close();
        }
    }

    @Override
    public void remove(String key) {
        ShardedJedis jedis = pool.getResource();
        try {
            Long entriesRemoved = jedis.del(key.getBytes());
            LOG.trace("remove - key: " + key + ", entriesRemoved: " + entriesRemoved);
        } finally {
            jedis.close();
        }
    }

    @Override
    public void clear() {
        LOG.trace("clear not supported by sharded implemented");
    }
}

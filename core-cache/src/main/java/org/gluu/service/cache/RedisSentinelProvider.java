package org.gluu.service.cache;

import com.google.common.collect.Sets;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;

import java.io.Serializable;

/**
 * Important : keep it weld free. It's reused by oxd !
 *
 * @author Yuriy Zabrovarnyy
 */
public class RedisSentinelProvider extends AbstractRedisProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RedisSentinelProvider.class);

    private JedisSentinelPool pool;

    public RedisSentinelProvider(RedisConfiguration redisConfiguration) {
        super(redisConfiguration);
    }

    public void create() {
        try {
            LOG.debug("Starting RedisSentinelProvider ... configuration:" + getRedisConfiguration());

            JedisPoolConfig poolConfig = createPoolConfig();
            String password = StringUtils.isBlank(redisConfiguration.getDecryptedPassword()) ? null : redisConfiguration.getDecryptedPassword();

            pool = new JedisSentinelPool(
                    getRedisConfiguration().getSentinelMasterGroupName(),
                    Sets.newHashSet(StringUtils.split(getRedisConfiguration().getServers().trim(), ",")),
                    poolConfig,
                    redisConfiguration.getConnectionTimeout(),
                    redisConfiguration.getSoTimeout(),
                    password,
                    Protocol.DEFAULT_DATABASE);

            testConnection();
            LOG.debug("RedisSentinelProvider started.");
        } catch (Exception e) {
            LOG.error("Failed to start RedisSentinelProvider.");
            throw new IllegalStateException("Error starting RedisSentinelProvider", e);
        }
    }

    public void destroy() {
        LOG.debug("Destroying RedisSentinelProvider");

        try {
            pool.close();
        } catch (Exception e) {
            LOG.error("Failed to destroy RedisSentinelProvider", e);
            return;
        }

        LOG.debug("Destroyed RedisSentinelProvider");
    }

    @Override
    public JedisSentinelPool getDelegate() {
        return pool;
    }

	@Override
	public boolean hasKey(String key) {
        Boolean hasKey = pool.getResource().exists(key);

        return Boolean.TRUE.equals(hasKey);
	}

    @Override
    public Object get(String key) {
        byte[] value = pool.getResource().get(key.getBytes());
        Object deserialized = null;
        if (value != null && value.length > 0) {
            deserialized = SerializationUtils.deserialize(value);
        }
        return deserialized;
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        String status = pool.getResource().setex(key.getBytes(), expirationInSeconds, SerializationUtils.serialize((Serializable) object));
        LOG.trace("put - key: " + key + ", status: " + status);
    }

    @Override
    public void put(String key, Object object) {
        String status = pool.getResource().set(key.getBytes(), SerializationUtils.serialize((Serializable) object));
        LOG.trace("put - key: " + key + ", status: " + status);
    }

    @Override
    public void remove(String key) {
        Long entriesRemoved = pool.getResource().del(key.getBytes());
        LOG.trace("remove - key: " + key + ", entriesRemoved: " + entriesRemoved);
    }

    @Override
    public void clear() {
        Jedis jedis = pool.getResource();

        try {
            jedis.flushAll();
            LOG.trace("clear");
        } finally {
            jedis.close();
        }
    }
}

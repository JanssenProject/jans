/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.io.Serializable;

import static io.jans.service.cache.RedisClusterProvider.hosts;

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
            String password = redisConfiguration.getPassword();
            JedisClientConfig jedisClientConfig;

            if (redisConfiguration.getUseSSL()) {
                RedisProviderFactory.setSSLSystemProperties(redisConfiguration);
                jedisClientConfig = DefaultJedisClientConfig.builder().ssl(true).password(password).build();
            } else {
                jedisClientConfig = DefaultJedisClientConfig.builder().ssl(false).password(password).build();
            }

            pool = new JedisSentinelPool(getRedisConfiguration().getSentinelMasterGroupName(),
                    hosts(getRedisConfiguration().getServers()), poolConfig, jedisClientConfig, jedisClientConfig);

            testConnection();
            LOG.debug("RedisSentinelProvider started.");
        } catch (Exception e) {
            LOG.error("Failed to start RedisSentinelProvider.", e);
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
        try (final Jedis resource = pool.getResource()) {
            Boolean hasKey = resource.exists(key);

            return Boolean.TRUE.equals(hasKey);
        }
	}

    @Override
    public Object get(String key) {
        try (final Jedis resource = pool.getResource()) {
            byte[] value = resource.get(key.getBytes());
            Object deserialized = null;
            if (value != null && value.length > 0) {
                deserialized = SerializationUtils.deserialize(value);
            }
            return deserialized;
        }
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        try (final Jedis resource = pool.getResource()) {
            String status = resource.setex(key.getBytes(), expirationInSeconds, SerializationUtils.serialize((Serializable) object));
            LOG.trace("put - key: " + key + ", status: " + status);
        }
    }

    @Override
    public void put(String key, Object object) {
        try (final Jedis resource = pool.getResource()) {
            String status = resource.set(key.getBytes(), SerializationUtils.serialize((Serializable) object));
            LOG.trace("put - key: " + key + ", status: " + status);
        }
    }

    @Override
    public void remove(String key) {
        try (final Jedis resource = pool.getResource()) {
            Long entriesRemoved = resource.del(key.getBytes());
            LOG.trace("remove - key: " + key + ", entriesRemoved: " + entriesRemoved);
        }
    }

    @Override
    public void clear() {
        try (final Jedis resource = pool.getResource()) {
            resource.flushAll();
            LOG.trace("clear");
        }
    }
}

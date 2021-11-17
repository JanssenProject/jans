/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;

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
            pool = new JedisSentinelPool(
                    getRedisConfiguration().getSentinelMasterGroupName(),
                    Set.of(StringUtils.split(getRedisConfiguration().getServers().trim(), ",")),
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

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import jakarta.annotation.PreDestroy;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.io.Serializable;

/**
 * Important : keep it weld free. It's reused by oxd !
 *
 * @author yuriyz on 02/23/2017.
 */
public class RedisStandaloneProvider extends AbstractRedisProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RedisStandaloneProvider.class);

    private JedisPool pool;
    public RedisStandaloneProvider(RedisConfiguration redisConfiguratio) {
        super(redisConfiguratio);
    }

    public void create() {
        LOG.debug("Starting RedisStandaloneProvider ...");

        try {
            JedisPoolConfig poolConfig = createPoolConfig();

            HostAndPort hostAndPort = RedisClusterProvider.hosts(redisConfiguration.getServers()).iterator().next();
            String password = redisConfiguration.getPassword();
            if (redisConfiguration.getUseSSL()) {
                if (StringUtils.isNotBlank(redisConfiguration.getSslTrustStoreFilePath())
                        && StringUtils.isNotBlank(redisConfiguration.getSslKeyStoreFilePath())) {
                    SSLSocketFactory sslSocketFactory = RedisProviderFactory.createSslSocketFactory(redisConfiguration);

                    pool = new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), true,
                            sslSocketFactory, new SSLParameters(), new DefaultHostnameVerifier());
                } else {
                    pool = new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(),
                            redisConfiguration.getConnectionTimeout(), password, true);
                }
            } else {
                pool = new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), redisConfiguration.getConnectionTimeout(), password);
            }

            testConnection();
            LOG.debug("RedisStandaloneProvider started.");
        } catch (Exception e) {
            LOG.error("Problems connecting with Redis", e);
            throw new IllegalStateException("Error starting RedisStandaloneProvider", e);
        }
    }

    @PreDestroy
    public void destroy() {
        LOG.debug("Destroying RedisStandaloneProvider");

        pool.close();

        LOG.debug("Destroyed RedisStandaloneProvider");
    }

    @Override
    public JedisPool getDelegate() {
        return pool;
    }

	@Override
	public boolean hasKey(String key) {
        Boolean hasKey = pool.getResource().exists(key);

        return Boolean.TRUE.equals(hasKey);
	}

    @Override
    public Object get(String key) {
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

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        Jedis jedis = pool.getResource();

        try {
            String status = jedis.setex(key.getBytes(), expirationInSeconds, SerializationUtils.serialize((Serializable) object));
            LOG.trace("put - key: " + key + ", status: " + status);
        } finally {
            jedis.close();
        }
    }

    @Override
    public void put(String key, Object object) {
        Jedis jedis = pool.getResource();

        try {
            String status = jedis.set(key.getBytes(), SerializationUtils.serialize((Serializable) object));
            LOG.trace("put - key: " + key + ", status: " + status);
        } finally {
            jedis.close();
        }
    }

    @Override
    public void remove(String key) {
        Jedis jedis = pool.getResource();

        try {
            Long entriesRemoved = jedis.del(key.getBytes());
            LOG.trace("remove - key: " + key + ", entriesRemoved: " + entriesRemoved);
        } finally {
            jedis.close();
        }
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

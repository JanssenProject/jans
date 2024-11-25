/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import javax.net.ssl.SSLParameters;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Important : keep it weld free. It's reused by oxd !
 *
 * @author yuriyz on 02/23/2017.
 */
public class RedisShardedProvider extends AbstractRedisProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RedisShardedProvider.class);

    private ShardedJedisPool pool;

    public RedisShardedProvider(RedisConfiguration redisConfiguration) {
        super(redisConfiguration);
    }

    public void create() {
        try {
            LOG.debug("Starting RedisShardedProvider ... configuration:" + redisConfiguration);

            GenericObjectPoolConfig<ShardedJedis> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(redisConfiguration.getMaxTotalConnections());
            poolConfig.setMaxIdle(redisConfiguration.getMaxIdleConnections());
            poolConfig.setMinIdle(2);

            pool = new ShardedJedisPool(poolConfig, shards(redisConfiguration));

            testConnection();
            LOG.debug("RedisShardedProvider started.");
        } catch (Exception e) {
            LOG.error("Failed to start RedisShardedProvider.", e);
            throw new IllegalStateException("Error starting RedisShardedProvider", e);
        }
    }

    private static List<JedisShardInfo> shards(RedisConfiguration configuration) {
        final String[] serverWithPorts = StringUtils.split(configuration.getServers().trim(), ",");

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        for (String serverWithPort : serverWithPorts) {
            serverWithPort = serverWithPort.trim();
            if (serverWithPort.contains(":") && !serverWithPort.contains("@") && !configuration.getServers().contains("//")) {
                final String[] split = serverWithPort.trim().split(":");
                String host = split[0];
                int port = Integer.parseInt(split[1].trim());

                try {
                    final JedisShardInfo shardInfo;
                    if (configuration.getUseSSL()) {
                        if (StringUtils.isNotBlank(configuration.getSslTrustStoreFilePath())
                                && StringUtils.isNotBlank(configuration.getSslKeyStoreFilePath())) {
                            shardInfo = new JedisShardInfo(host, port, true,
                                    RedisProviderFactory.createSslSocketFactory(configuration),
                                    new SSLParameters(), new DefaultHostnameVerifier());
                        } else {
                            shardInfo = new JedisShardInfo(host, port, true);
                        }
                    } else {
                        shardInfo = new JedisShardInfo(host, port);
                    }
                    shards.add(shardInfo);
                } catch (Exception e) {
                    LOG.error("Failed to create shard info.", e);
                }
            } else {
                shards.add(new JedisShardInfo(serverWithPort));
            }
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
	public boolean hasKey(String key) {
        try (final ShardedJedis resource = pool.getResource()) {
            Boolean hasKey = resource.exists(key);

            return Boolean.TRUE.equals(hasKey);
        }
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
            String status = jedis.setex(key.getBytes(), expirationInSeconds, SerializationUtils.serialize((Serializable) object));
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

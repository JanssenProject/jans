/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author yuriyz
 */
public abstract class AbstractRedisProvider {

	protected RedisConfiguration redisConfiguration;

	public AbstractRedisProvider(RedisConfiguration redisConfiguration) {
		this.redisConfiguration = redisConfiguration;
        HostAndPort.setLocalhost("127.0.0.1");
	}

	public JedisPoolConfig createPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisConfiguration.getMaxTotalConnections());
        poolConfig.setMaxIdle(redisConfiguration.getMaxIdleConnections());
        poolConfig.setMinIdle(2);
        return poolConfig;
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

	public boolean isConnected() {
		put(2, "testKey", "testValue");
		if (!"testValue".equals(get("testKey"))) {
			return false;
		}
		return true;
	}

	public abstract void create();

	public abstract void destroy();

	public abstract Object getDelegate();
	
	public abstract boolean hasKey(String key);

	public abstract Object get(String key);

	public abstract void remove(String key);

	public abstract void clear();

	public abstract void put(int expirationInSeconds, String key, Object object);

	public abstract void put(String key, Object object);

}

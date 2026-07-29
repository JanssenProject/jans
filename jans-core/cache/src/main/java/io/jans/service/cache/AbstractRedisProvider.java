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

	protected CacheConfiguration cacheConfiguration;
	protected RedisConfiguration redisConfiguration;

	public AbstractRedisProvider(CacheConfiguration cacheConfiguration) {
		this.cacheConfiguration = cacheConfiguration;
		this.redisConfiguration = cacheConfiguration.getRedisConfiguration();
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

	private String getTestKey() {
		String keyPrefix = cacheConfiguration.getKeyPrefix();
		if (keyPrefix == null) {
			keyPrefix = "";
		}
		return keyPrefix + "testKey";
	}

	public void testConnection() {
		String key = getTestKey();
		put(2, key, "testValue");
		if (!"testValue".equals(get(key))) {
			throw new RuntimeException("Failed to connect to redis server. Configuration: " + redisConfiguration);
		}
	}

	public boolean isConnected() {
		String key = getTestKey();
		put(2, key, "testValue");
		if (!"testValue".equals(get(key))) {
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

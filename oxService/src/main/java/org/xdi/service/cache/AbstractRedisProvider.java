package org.xdi.service.cache;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.security.StringEncrypter;
import org.xdi.util.security.StringEncrypter.EncryptionException;

import redis.clients.jedis.Jedis;

/**
 * @author yuriyz
 */
public abstract class AbstractRedisProvider {
	private final static Logger LOG = LoggerFactory.getLogger(AbstractRedisProvider.class);

	protected RedisConfiguration redisConfiguration;

	@Inject
	private StringEncrypter stringEncrypter;

	public AbstractRedisProvider(RedisConfiguration redisConfiguration) {
		this.redisConfiguration = redisConfiguration;
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

	public void setAuthIfNeeded(Jedis jedis) {
		String encryptedPassword = redisConfiguration.getPassword();
		if (StringUtils.isNotBlank(encryptedPassword)) {
			try {
				jedis.auth(stringEncrypter.decrypt(encryptedPassword));
			} catch (EncryptionException e) {
				LOG.error("Error during redis password decryption", e);
			}
		}
	}

	public abstract void create();

	public abstract void destroy();

	public abstract Object getDelegate();

	public abstract Object get(String key);

	public abstract void remove(String key);

	public abstract void clear();

	public abstract void put(int expirationInSeconds, String key, Object object);

	public abstract void put(String key, Object object);

}

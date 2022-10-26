/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author yuriyz on 02/23/2017.
 */
@ApplicationScoped
public class RedisProvider extends AbstractCacheProvider<AbstractRedisProvider> {

    public static final int DEFAULT_PUT_EXPIRATION_IN_SECONDS = 60;

    @Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    @Inject
    private StringEncrypter stringEncrypter;

    private AbstractRedisProvider redisProvider;
    private int defaultPutExpiration = DEFAULT_PUT_EXPIRATION_IN_SECONDS;

    public RedisProvider() {
    }

    @PostConstruct
    public void init() {
    }

    public void create() {
        try {
            RedisConfiguration redisConfiguration = cacheConfiguration.getRedisConfiguration();
            decryptPassword(redisConfiguration);
            log.debug("Starting RedisProvider ... configuration:" + redisConfiguration);
            defaultPutExpiration = redisConfiguration.getDefaultPutExpiration() > 0 ? redisConfiguration.getDefaultPutExpiration()
                    : DEFAULT_PUT_EXPIRATION_IN_SECONDS;
            redisProvider = RedisProviderFactory.create(cacheConfiguration.getRedisConfiguration());
            redisProvider.create();
            log.debug("RedisProvider started.");
        } catch (Exception e) {
            log.error("Failed to start RedisProvider.");
            log.error("Log Error",e);
            throw new IllegalStateException("Error starting RedisProvider", e);
        }
    }

	public void configure(CacheConfiguration cacheConfiguration, StringEncrypter stringEncrypter) {
		this.log = LoggerFactory.getLogger(RedisProvider.class);
		this.cacheConfiguration = cacheConfiguration;
		this.stringEncrypter = stringEncrypter;
	}

    public void setCacheConfiguration(CacheConfiguration cacheConfiguration){
        this.cacheConfiguration=cacheConfiguration;
    }

    private void decryptPassword(RedisConfiguration redisConfiguration) {
        try {
            String encryptedPassword = redisConfiguration.getPassword();
            if (StringUtils.isNotBlank(encryptedPassword)) {
                redisConfiguration.setPassword(stringEncrypter.decrypt(encryptedPassword));
                log.trace("Decrypted redis password successfully.");
            }
        } catch (StringEncrypter.EncryptionException e) {
            log.error("Error during redis password decryption", e);
        }
    }

    public boolean isConnected(){
        return redisProvider.isConnected();
    }


    @PreDestroy
    public void destroy() {
        log.debug("Destroying RedisProvider");
        redisProvider.destroy();
        log.debug("Destroyed RedisProvider");
    }

    @Override
    public AbstractRedisProvider getDelegate() {
        return redisProvider;
    }

	@Override
	public boolean hasKey(String key) {
        if (key == null) {
            return false;
        }
        return redisProvider.hasKey(key);
	}

    @Override
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        return redisProvider.get(key);
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        redisProvider.put(expirationInSeconds > 0 ? expirationInSeconds : defaultPutExpiration, key, object);
    }

    @Override
    public void remove(String key) {
        redisProvider.remove(key);
    }

    @Override
    public void clear() {
        redisProvider.clear();
    }

    @Override
    public CacheProviderType getProviderType() {
        return CacheProviderType.REDIS;
    }

}

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import io.jans.service.cache.CacheInterface;
import io.jans.service.cache.CacheProvider;
import io.jans.service.cache.CacheProviderType;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.util.Date;
import java.util.function.Supplier;

/**
 * Provides operations with cache
 *
 * @author Yuriy Movchan Date: 01.24.2012
 * @author Yuriy Zabrovarnyy Date: 02.02.2017
 */
public abstract class BaseCacheService implements CacheInterface {
	
	public static int DEFAULT_EXPIRATION = 60;

	@Inject
    private Logger log;

    public Object get(String key) {
    	CacheProvider cacheProvider = getCacheProvider();
        if (cacheProvider == null) {
        	log.error("Cache provider is invalid!");
            return null;
        }

    	log.trace("Request data, key '{}'", key);
    	Object value = cacheProvider.get(key);
    	log.trace("Loaded data, key '{}': '{}'", key, value);

    	return value;
    }

    public <T> T getWithPut(String key, Supplier<T> loadFunction, int expirationInSeconds) {
        if (loadFunction == null) {
            return (T) get(key);
        }

    	CacheProvider cacheProvider = getCacheProvider();

    	if (CacheProviderType.NATIVE_PERSISTENCE == cacheProvider.getProviderType()) {
        	log.trace("Loading data from DB without cache, key '{}'", key);
            return loadFunction.get();
        }

        final Object value = get(key);
        if (value != null) {
            log.trace("Loaded from cache, key: '{}'", key);
            return (T) value;
        } else {
            log.trace("Key not in cache. Searching value via load function, key: '{}'", key);
            final T loaded = loadFunction.get();
            if (loaded == null) {
                log.trace("Key not in cache. There is no value, key: '{}'", key);
                return null;
            }

            try {
                put(expirationInSeconds, key, loaded);
            } catch (Exception e) {
                log.error("Failed to put object into cache, key: '{}'", key, e); // we don't want prevent returning loaded value due to failure with put
            }
            return loaded;
        }
    }

	public void put(int expirationInSeconds, String key, Object object) {
    	CacheProvider cacheProvider = getCacheProvider();
    	if (cacheProvider == null) {
        	log.error("Cache provider is invalid!");
			return;
		}
		
    	log.trace("Put data, key '{}': '{}'", key, object);
		cacheProvider.put(expirationInSeconds, key, object);
	}

	public void remove(String key) {
    	CacheProvider cacheProvider = getCacheProvider();
    	if (cacheProvider == null) {
        	log.error("Cache provider is invalid!");
			return;
		}
		
    	log.trace("Remove data, key '{}'", key);
		cacheProvider.remove(key);
	}

	public void clear() {
    	CacheProvider cacheProvider = getCacheProvider();
    	if (cacheProvider == null) {
        	log.error("Cache provider is invalid!");
			return;
		}

    	log.trace("Clear cache");
		cacheProvider.clear();
	}

    @Override
    public void cleanup(Date now) {
    	CacheProvider cacheProvider = getCacheProvider();
    	if (cacheProvider == null) {
        	log.error("Cache provider is invalid!");
			return;
		}

    	log.trace("Clean up cache");
        cacheProvider.cleanup(now);
    }

    public void put(String key, Object object) {
        put(DEFAULT_EXPIRATION, key, object);
    }

    protected abstract CacheProvider getCacheProvider();

}

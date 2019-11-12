/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.service;

import java.util.Date;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.gluu.service.cache.CacheInterface;
import org.gluu.service.cache.CacheProvider;
import org.gluu.service.cache.CacheProviderType;
import org.slf4j.Logger;

/**
 * Provides operations with cache
 *
 * @author Yuriy Movchan Date: 01.24.2012
 * @author Yuriy Zabrovarnyy Date: 02.02.2017
 */
public abstract class BaseCacheService implements CacheInterface {
	
	private static int DEFAULT_EXPIRATION = 60;

	@Inject
    private Logger log;

    public Object get(String key) {
    	CacheProvider cacheProvider = getCacheProvider();
        if (cacheProvider == null) {
            return null;
        }

        return cacheProvider.get(key);
    }

    public <T> T getWithPut(String key, Supplier<T> loadFunction, int expirationInSeconds) {
        if (loadFunction == null) {
            return (T) get(key);
        }

    	CacheProvider cacheProvider = getCacheProvider();

    	if (CacheProviderType.NATIVE_PERSISTENCE == cacheProvider.getProviderType()) {
            return loadFunction.get();
        }

        final Object value = get(key);
        if (value != null) {
            log.trace("Loaded from cache, key: " + key);
            return (T) value;
        } else {
            final T loaded = loadFunction.get();
            if (loaded == null) {
                return null;
            }

            try {
                put(expirationInSeconds, key, loaded);
            } catch (Exception e) {
                log.error("Failed to put object into cache, key: " + key, e); // we don't want prevent returning loaded value due to failure with put
            }
            return loaded;
        }
    }

	public void put(int expirationInSeconds, String key, Object object) {
    	CacheProvider cacheProvider = getCacheProvider();

    	if (cacheProvider != null) {
			cacheProvider.put(expirationInSeconds, key, object);
		}
	}

	public void remove(String key) {
    	CacheProvider cacheProvider = getCacheProvider();

    	if (cacheProvider == null) {
			return;
		}
		
		cacheProvider.remove(key);
	}

	public void clear() {
    	CacheProvider cacheProvider = getCacheProvider();

    	if (cacheProvider != null) {
			cacheProvider.clear();
		}
	}

    @Override
    public void cleanup(Date now) {
    	CacheProvider cacheProvider = getCacheProvider();

    	if (cacheProvider != null) {
            cacheProvider.cleanup(now);
        }
    }

    public void put(String key, Object object) {
        put(DEFAULT_EXPIRATION, key, object);
    }

    @Deprecated // we keep it only for back-compatibility of scripts code
    public Object get(String region, String key) {
        return get(key);
    }

    @Deprecated // we keep it only for back-compatibility of scripts code
    public void put(String expirationInSeconds, String key, Object object) {
    	int expiration = DEFAULT_EXPIRATION; 
    	try {
			expiration = Integer.parseInt(expirationInSeconds);
		} catch (NumberFormatException ex) {
			// Use default expiration
			log.trace("Using default expiration instead of expirationInSeconds: {}", expirationInSeconds);
		}
        put(expiration, key, object);
    }

    @Deprecated // we keep it only for back-compatibility of scripts code
    public void remove(String region, String key) {
        remove(key);
    }

    protected abstract CacheProvider getCacheProvider();

}

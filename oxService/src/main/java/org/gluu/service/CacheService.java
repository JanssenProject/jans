/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.service;

import org.gluu.service.cache.CacheInterface;
import org.gluu.service.cache.CacheProvider;
import org.gluu.service.cache.CacheProviderType;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;

/**
 * Provides operations with cache
 *
 * @author Yuriy Movchan Date: 01.24.2012
 * @author Yuriy Zabrovarnyy Date: 02.02.2017
 */
@ApplicationScoped
@Named
public class CacheService implements CacheInterface {

    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private Logger log;

    public Object get(String key) {
        if (cacheProvider == null) {
            return null;
        }

        return cacheProvider.get(key);
    }

    public void put(int expirationInSeconds, String key, Object object) {
        put(expirationInSeconds, key, object, false);
    }

	public void put(int expirationInSeconds, String key, Object object, boolean skipPutOnNativePersistence) {
		if (skipPutOnNativePersistence && (CacheProviderType.NATIVE_PERSISTENCE == cacheProvider.getProviderType())) {
			return;
		}
		if (cacheProvider != null) {
			cacheProvider.put(expirationInSeconds, key, object);
		}
	}

	public void remove(String key) {
		if (cacheProvider == null) {
			return;
		}
		
		cacheProvider.remove(key);
	}

	public void clear() {
		if (cacheProvider != null) {
			cacheProvider.clear();
		}
	}

    @Override
    public void cleanup(Date now) {
        if (cacheProvider != null) {
            cacheProvider.cleanup(now);
        }
    }

    @Deprecated // we keep it only for back-compatibility of scripts code
    public Object get(String region, String key) {
        return get(key);
    }

    @Deprecated // we keep it only for back-compatibility of scripts code
    public void put(String expirationInSeconds, String key, Object object) {
        put(Integer.parseInt(expirationInSeconds), key, object);
    }

    @Deprecated // we keep it only for back-compatibility of scripts code
    public void remove(String region, String key) {
        remove(key);
    }
}

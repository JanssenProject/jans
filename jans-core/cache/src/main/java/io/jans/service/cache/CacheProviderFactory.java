/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;

/**
 * @author yuriyz on 02/21/2017.
 */
@ApplicationScoped
@Named
public class CacheProviderFactory {

    @Inject
    private Logger log;

    @Inject
    private CacheConfiguration cacheConfiguration;

    @Inject
    @Any
    private Instance<CacheProvider> instance;

    @Produces
    @ApplicationScoped
    public CacheProvider getCacheProvider() {
        log.debug("Started to create cache provider");


        return getCacheProvider(cacheConfiguration);
    }

    public CacheProvider getCacheProvider(CacheConfiguration cacheConfiguration) {
		CacheProviderType cacheProviderType = cacheConfiguration.getCacheProviderType();

        if (cacheProviderType == null) {
            log.error("Failed to initialize cacheProvider, cacheProviderType is null. Fallback to IN_MEMORY type.");
            cacheProviderType = CacheProviderType.IN_MEMORY;
        }

        // Create proxied bean
        AbstractCacheProvider<?> cacheProvider = null;
        switch (cacheProviderType) {
            case IN_MEMORY:
            	cacheProvider = instance.select(InMemoryCacheProvider.class).get();
                break;
            case MEMCACHED:
            	cacheProvider = instance.select(MemcachedProvider.class).get();
                break;
            case REDIS:
            	cacheProvider = instance.select(RedisProvider.class).get();
                break;
            case NATIVE_PERSISTENCE:
                cacheProvider = instance.select(NativePersistenceCacheProvider.class).get();
                break;
        }

        if (cacheProvider == null) {
            throw new RuntimeException("Failed to initialize cacheProvider, cacheProviderType is unsupported: " + cacheProviderType);
        }

        cacheProvider.create();

        return cacheProvider;
	}

    @Produces
    @ApplicationScoped
    @LocalCache
    public CacheProvider getLocalCacheProvider() {
        log.debug("Started to create local cache provider");

        CacheProviderType cacheProviderType = CacheProviderType.IN_MEMORY;
        AbstractCacheProvider<?> cacheProvider = instance.select(InMemoryCacheProvider.class).get();

        if (cacheProvider == null) {
            throw new RuntimeException("Failed to initialize cacheProvider, cacheProviderType is unsupported: " + cacheProviderType);
        }

        cacheProvider.create();

        return cacheProvider;
    }

}

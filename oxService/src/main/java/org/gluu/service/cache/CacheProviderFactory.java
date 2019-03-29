package org.gluu.service.cache;

import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

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

        AbstractCacheProvider<?> cacheProvider = null;

        CacheProviderType cacheProviderType = cacheConfiguration.getCacheProviderType();

        if (cacheProviderType == null) {
            log.error("Failed to initialize cacheProvider, cacheProviderType is null. Fallback to IN_MEMORY type.");
            cacheProviderType = CacheProviderType.IN_MEMORY;
        }

        // Create proxied bean
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

}

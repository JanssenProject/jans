package org.xdi.service.cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

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

    @Produces @ApplicationScoped
    public CacheProvider getCacheProvider() {
        log.debug("Started to create cache provider");

        AbstractCacheProvider<?> cacheProvider = null;

        CacheProviderType cacheProviderType = cacheConfiguration.getCacheProviderType();

        if (cacheProviderType == null) {
            log.error("Failed to initialize cacheProvider, cacheProviderType is null. Fallback to IN_MEMORY type.");
            cacheProviderType = CacheProviderType.IN_MEMORY;
        }

        switch (cacheProviderType) {
            case IN_MEMORY:
                cacheProvider = new InMemoryCacheProvider(cacheConfiguration.getInMemoryConfiguration());
                break;
            case MEMCACHED:
                cacheProvider = new MemcachedProvider(cacheConfiguration.getMemcachedConfiguration());
                break;
            case REDIS:
                cacheProvider = new RedisProvider(cacheConfiguration.getRedisConfiguration());
                break;
        }

        if (cacheProvider == null) {
            throw new RuntimeException("Failed to initialize cacheProvider, cacheProviderType is unsupported: " + cacheProviderType);
        }

        cacheProvider.create();
        
        return cacheProvider;
    }

}

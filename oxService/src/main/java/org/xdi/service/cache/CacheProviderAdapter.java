package org.xdi.service.cache;

import org.jboss.seam.annotations.*;
import org.jboss.seam.cache.CacheProvider;
import org.jboss.seam.log.Log;

import static org.jboss.seam.ScopeType.APPLICATION;

/**
 * @author yuriyz on 02/21/2017.
 */
@Name("cachedProviderAdapter")
@Scope(APPLICATION)
@AutoCreate
@Startup
public class CacheProviderAdapter extends AbstractCacheProvider<CacheProvider> {

    @Logger
    private Log log;

    @In(required = true)
    private CacheConfiguration cacheConfiguration;

    private AbstractCacheProvider cacheProvider = null;

    @Create
    public void create() {
        log.debug("Started to create cache provider");

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
    }

    @Destroy
    public void destroy() {
        cacheProvider.destroy();
    }

    @Override
    public CacheProvider getDelegate() {
        return cacheProvider;
    }

    @Override
    public Object get(String region, String key) {
        return cacheProvider.get(region, key);
    }

    @Override
    public void put(String region, String key, Object object) {
        cacheProvider.put(region, key, object);
    }

    @Override
    public void remove(String region, String key) {
        cacheProvider.remove(region, key);
    }

    @Override
    public void clear() {
        cacheProvider.clear();
    }
}

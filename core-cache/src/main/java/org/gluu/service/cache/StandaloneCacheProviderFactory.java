package org.gluu.service.cache;

import org.gluu.persist.PersistenceEntryManager;
import org.gluu.util.security.StringEncrypter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public class StandaloneCacheProviderFactory {

	private static final Logger LOG = LoggerFactory.getLogger(StandaloneCacheProviderFactory.class);

	private PersistenceEntryManager entryManager;
	private StringEncrypter stringEncrypter;

	public StandaloneCacheProviderFactory(PersistenceEntryManager entryManager, StringEncrypter stringEncrypter) {
		this.entryManager = entryManager;
		this.stringEncrypter = stringEncrypter;
	}

	public CacheProvider<?> getCacheProvider(CacheConfiguration cacheConfiguration) {
		CacheProviderType cacheProviderType = cacheConfiguration.getCacheProviderType();

		if (cacheProviderType == null) {
			LOG.error("Failed to initialize cacheProvider, cacheProviderType is null. Fallback to IN_MEMORY type.");
			cacheProviderType = CacheProviderType.IN_MEMORY;
		}

		// Create bean
		AbstractCacheProvider<?> cacheProvider = null;
		switch (cacheProviderType) {
			case IN_MEMORY:
				InMemoryCacheProvider inMemoryCacheProvider = new InMemoryCacheProvider();
				inMemoryCacheProvider.configure(cacheConfiguration);
				inMemoryCacheProvider.init();
	
				cacheProvider = inMemoryCacheProvider;
				break;
			case MEMCACHED:
				MemcachedProvider memcachedProvider = new MemcachedProvider();
				memcachedProvider.configure(cacheConfiguration);
				memcachedProvider.init();
	
				cacheProvider = memcachedProvider;
				break;
			case REDIS:
				RedisProvider redisProvider = new RedisProvider();
				redisProvider.configure(cacheConfiguration, stringEncrypter);
				redisProvider.init();
	
				cacheProvider = redisProvider;
				break;
			case NATIVE_PERSISTENCE:
				NativePersistenceCacheProvider nativePersistenceCacheProvider = new NativePersistenceCacheProvider();
				nativePersistenceCacheProvider.configure(cacheConfiguration, entryManager);
				nativePersistenceCacheProvider.init();
	
				cacheProvider = nativePersistenceCacheProvider;
				break;
		}

		if (cacheProvider == null) {
			throw new RuntimeException("Failed to initialize cacheProvider, cacheProviderType is unsupported: " + cacheProviderType);
		}

		cacheProvider.create();

		return cacheProvider;
	}

}

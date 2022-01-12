/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import io.jans.orm.PersistenceEntryManager;

import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter;
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
				if (stringEncrypter == null) {
					throw new RuntimeException("Factory is not initialized properly. stringEncrypter is not specified");
				}

				RedisProvider redisProvider = new RedisProvider();
				redisProvider.configure(cacheConfiguration, stringEncrypter);
				redisProvider.init();
	
				cacheProvider = redisProvider;
				break;
			case NATIVE_PERSISTENCE:
				if (entryManager == null) {
					throw new RuntimeException("Factory is not initialized properly. entryManager is not specified");
				}

				NativePersistenceCacheProvider nativePersistenceCacheProvider = new NativePersistenceCacheProvider();
				nativePersistenceCacheProvider.configure(cacheConfiguration, entryManager);
				
				// TODO: Remove after configuration fix
				if (StringHelper.isEmpty(cacheConfiguration.getNativePersistenceConfiguration().getBaseDn())) {
					cacheConfiguration.getNativePersistenceConfiguration().setBaseDn("o=jans");
				}

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

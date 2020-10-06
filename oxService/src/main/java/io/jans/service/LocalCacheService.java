/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package io.jans.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.jans.service.cache.CacheProvider;
import io.jans.service.cache.LocalCache;

/**
 * Provides operations with cache
 *
 * @author Yuriy Movchan Date: 01.24.2012
 * @author Yuriy Zabrovarnyy Date: 02.02.2017
 */
@ApplicationScoped
@Named
public class LocalCacheService extends BaseCacheService {

    @Inject
    @LocalCache
    private CacheProvider cacheProvider;

	@Override
	protected CacheProvider getCacheProvider() {
		return cacheProvider;
	}


}

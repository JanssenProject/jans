/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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

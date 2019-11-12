/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.service.cache.CacheProvider;
import org.gluu.service.cache.LocalCache;

/**
 * Provides operations with cache
 *
 * @author Yuriy Movchan Date: 01.24.2012
 * @author Yuriy Zabrovarnyy Date: 02.02.2017
 */
@ApplicationScoped
@Named
@LocalCache
public class LocalCacheService extends CacheService {

    @Inject
    @LocalCache
    private CacheProvider cacheProvider;

	@Override
	public CacheProvider getCacheProvider() {
		return cacheProvider;
	}


}

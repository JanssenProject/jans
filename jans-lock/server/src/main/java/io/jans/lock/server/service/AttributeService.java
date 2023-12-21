/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.server.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.service.BaseCacheService;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import io.jans.util.StringHelper;

/**
 * 
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
public class AttributeService extends io.jans.service.AttributeService {

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private StaticConfiguration staticConfiguration;

    @Inject
    protected CacheService cacheService;

    @Inject
    protected LocalCacheService localCacheService;

    protected boolean isUseLocalCache() {
    	return true;
    }

	@Override
	protected BaseCacheService getCacheService() {
        if (isUseLocalCache()) {
            return localCacheService;
        }
        return cacheService;
	}

	@Override
    public String getDnForAttribute(String inum) {
        String attributesDn = staticConfiguration.getBaseDn().getAttributes();
        if (StringHelper.isEmpty(inum)) {
            return attributesDn;
        }
        return String.format("inum=%s,%s", inum, attributesDn);
    }

}
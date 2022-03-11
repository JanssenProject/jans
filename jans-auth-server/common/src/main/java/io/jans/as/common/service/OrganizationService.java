/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service;

import io.jans.as.persistence.model.GluuOrganization;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.BaseCacheService;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import io.jans.util.OxConstants;

import jakarta.inject.Inject;

public abstract class OrganizationService extends io.jans.service.OrganizationService {

    public static final int ONE_MINUTE_IN_SECONDS = 60;
    private static final long serialVersionUID = -8966940469789981584L;
    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private CacheService cacheService;

    @Inject
    private LocalCacheService localCacheService;

    /**
     * Update organization entry
     *
     * @param organization Organization
     */
    public void updateOrganization(GluuOrganization organization) {
        ldapEntryManager.merge(organization);
    }

    public GluuOrganization getOrganization() {
        BaseCacheService usedCacheService = getCacheService();
        return usedCacheService.getWithPut(OxConstants.CACHE_ORGANIZATION_KEY + "_oxauth", () -> ldapEntryManager.find(GluuOrganization.class, getDnForOrganization()), ONE_MINUTE_IN_SECONDS);
    }

    public String getDnForOrganization() {
        return "o=jans";
    }

    private BaseCacheService getCacheService() {
        if (isUseLocalCache()) {
            return localCacheService;
        }

        return cacheService;
    }

    protected abstract boolean isUseLocalCache();

}

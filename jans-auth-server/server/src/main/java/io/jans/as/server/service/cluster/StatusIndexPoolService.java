/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.token.StatusListIndexService;
import io.jans.model.token.StatusIndexPool;
import io.jans.model.token.StatusIndexPoolData;
import io.jans.model.token.StatusIndexPoolExpiration;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cdi.util.CdiUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class StatusIndexPoolService extends AbstractStatusIndexPoolService<StatusIndexPool> {

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private Logger log;

    @Override
    public int getStatusListBitSize() {
        return appConfiguration.getStatusListBitSize();
    }

    @Override
    public Class<StatusIndexPool> getEntityClass() {
        return StatusIndexPool.class;
    }

    @Override
    public void markAllIndexesAsValid(List<Integer> enumerateAllIndexes) {
        // mark all indexes which we are re-using as VALID
        StatusListIndexService indexService = CdiUtil.bean(StatusListIndexService.class);
        indexService.updateStatusAtIndexes(enumerateAllIndexes, TokenStatus.VALID);
    }

    @Override
    public long getMaxTokenLifetimeInSeconds() {
        long max = Math.max(appConfiguration.getAccessTokenLifetime(), appConfiguration.getIdTokenLifetime());
        final Integer logoutStatusJwtLifetime = appConfiguration.getLogoutStatusJwtLifetime();
        if (logoutStatusJwtLifetime != null) {
            max = Math.max(max, logoutStatusJwtLifetime);
        }
        return max;
    }

    /**
     * Calls are serialized within this node, and each write is verified by reading the stored value back. If a
     * concurrent writer stored a lower value, the write is repeated, so a raised expiration is not lost.
     */
    @Override
    public synchronized Date extendExpiration(String poolDn, Date expirationDate) {
        Date stored = null;
        for (int attempt = 1; attempt <= ATTEMPT_LIMIT; attempt++) {
            final StatusIndexPoolExpiration entry = entryManager.find(StatusIndexPoolExpiration.class, poolDn);
            if (entry == null) {
                return null;
            }

            final Date current = entry.getExpirationDate();
            if (current != null && !current.before(expirationDate)) {
                return current;
            }

            entry.setExpirationDate(expirationDate);
            entry.setLastUpdate(new Date());
            entryManager.merge(entry);

            final StatusIndexPoolExpiration reloaded = entryManager.find(StatusIndexPoolExpiration.class, poolDn);
            stored = reloaded != null ? reloaded.getExpirationDate() : null;
            if (stored != null && !stored.before(expirationDate)) {
                log.debug("{} Extended expiration of status index pool {} from {} to {}, attempt {}", logPrefix(), poolDn, current, stored, attempt);
                return stored;
            }
            log.debug("{} Expiration of status index pool {} was lowered concurrently to {}, attempt {}", logPrefix(), poolDn, stored, attempt);
        }

        log.error("{} Unable to extend expiration of status index pool {} to {}", logPrefix(), poolDn, expirationDate);
        return stored;
    }

    @Override
    public void updateData(StatusIndexPool pool) {
        final StatusIndexPoolData entry = new StatusIndexPoolData();
        entry.setDn(pool.getDn());
        entry.setData(pool.getData());
        entry.setLockKey(pool.getLockKey());
        entry.setLastUpdate(pool.getLastUpdate());
        entryManager.merge(entry);
    }

    @Override
    public String logPrefix() {
        return "[TokenIndex] - ";
    }

    public String baseDn() {
        return staticConfiguration.getBaseDn().getStatusIndexPool();
    }
}
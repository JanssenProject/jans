/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.token.StatusIndexPool;
import io.jans.model.token.TokenEntity;
import io.jans.model.tokenstatus.StatusList;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.orm.PersistenceEntryManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Marks a token's status list index invalid so status-aware verifiers reject it
 * after its persisted entry is removed. Mirrors the auth server's optimistic
 * lock protocol on the shared status index pool entries.
 */
@ApplicationScoped
public class TokenStatusListService {

    private static final String LOCK_KEY = UUID.randomUUID().toString();
    private static final int ATTEMPT_LIMIT = 10;

    @Inject
    private Logger logger;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private ConfigurationService configurationService;

    /**
     * @return {@code true} if the token has no status list index or it was marked invalid
     */
    public synchronized boolean invalidate(TokenEntity tokenEntity) {
        Integer index = tokenEntity.getAttributes() != null ? tokenEntity.getAttributes().getStatusListIndex() : null;
        if (index == null || index < 0) {
            return true;
        }

        try {
            AppConfiguration appConfiguration = configurationService.find();
            int blockSize = appConfiguration.getStatusListIndexAllocationBlockSize();
            String baseDn = staticConfiguration.getBaseDn().getStatusIndexPool();
            if (blockSize <= 0 || StringUtils.isBlank(baseDn)) {
                logger.error("Status list index pool is not configured, cannot invalidate index:{}", index);
                return false;
            }

            String poolDn = String.format("jansNum=%d,%s", index / blockSize, baseDn);
            if (!persistenceEntryManager.contains(poolDn, StatusIndexPool.class)) {
                // Pool expired and was not reused, so the index is no longer published
                logger.debug("Status index pool {} not found for index:{}", poolDn, index);
                return true;
            }
            return updateWithLock(poolDn, index, appConfiguration.getStatusListBitSize());
        } catch (Exception ex) {
            logger.error("Failed to invalidate status list index:{}", index, ex);
            return false;
        }
    }

    private boolean updateWithLock(String poolDn, int index, int bitSize) throws IOException {
        for (int attempt = 1; attempt < ATTEMPT_LIMIT; attempt++) {
            StatusIndexPool pool = persistenceEntryManager.find(StatusIndexPool.class, poolDn);
            StatusList statusList = StringUtils.isNotBlank(pool.getData())
                    ? StatusList.fromEncoded(pool.getData(), bitSize)
                    : new StatusList(bitSize);
            statusList.set(index, TokenStatus.INVALID.getValue());

            pool.setLockKey(LOCK_KEY);
            pool.setData(statusList.getLst());
            pool.setLastUpdate(new Date());
            persistenceEntryManager.merge(pool);

            StatusIndexPool reloaded = persistenceEntryManager.find(StatusIndexPool.class, poolDn);
            // another writer may have merged stale data after us, so the bit itself must be checked
            if (LOCK_KEY.equals(reloaded.getLockKey()) && isInvalid(reloaded, index, bitSize)) {
                return true;
            }
            logger.debug("Status index pool {} lock lost on attempt {}", poolDn, attempt);
        }
        logger.error("Unable to update status index pool {} for index:{}", poolDn, index);
        return false;
    }

    private static boolean isInvalid(StatusIndexPool pool, int index, int bitSize) throws IOException {
        return StringUtils.isNotBlank(pool.getData())
                && StatusList.fromEncoded(pool.getData(), bitSize).get(index) == TokenStatus.INVALID.getValue();
    }
}

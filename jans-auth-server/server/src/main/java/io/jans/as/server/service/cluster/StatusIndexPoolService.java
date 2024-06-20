/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.token.StatusIndexPool;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static io.jans.as.server.service.cluster.ClusterNodeService.LOCK_KEY;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class StatusIndexPoolService {

    public static long DELAY_AFTER_EXPIRATION = 3 * 60 * 60 * 1000L; // 3 hours
    public static long LOCK_WAIT_BEFORE_UPDATE = 3 * 1000L; // 30 seconds
    public static long DELAY_IF_LOCKED = 500; // 50 milliseconds
    public static final int ATTEMPT_LIMIT = 10;

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private PersistenceEntryManager entryManager;

    // Don't allow to change it after server start up. After setting new value we need to restart cluster
    private int indexAllocationBlockSize;

    @PostConstruct
    public void init() {
        log.info("Initializing Token Pool Service ...");
        indexAllocationBlockSize = appConfiguration.getStatusListIndexAllocationBlockSize();
    }

    /**
     * Returns pool by dn
     *
     * @return pool
     */
    public StatusIndexPool getPoolByDn(String dn) {
        return setIndexes(entryManager.find(StatusIndexPool.class, dn));
    }

    /**
     * Returns pool by Id
     *
     * @return pool
     */
    public StatusIndexPool getPoolById(int id) {
        return getPoolByDn(createDn(id));
    }

    /**
     * Returns a list of all pools
     *
     * @return list of pools
     */
    public List<StatusIndexPool> getAllPools() {
        return setIndexes(entryManager.findEntries(baseDn(), StatusIndexPool.class, Filter.createPresenceFilter("jansNum")));
    }

    /**
     * Returns last (max) pool or null if none
     *
     * @return pool
     */
    public StatusIndexPool getPoolLast() {
        final List<StatusIndexPool> all = getAllPools();
        final StatusIndexPool max = Collections.max(all, Comparator.comparing(StatusIndexPool::getId));
        log.debug("Last node: {}", max);
        return max;

        // todo - we need to use paged version when it is fixed in entry manager
//		PagedResult<StatusIndexPool> pagedResult = entryManager.findPagedEntries(baseDn, StatusIndexPool.class, Filter.createEqualityFilter("jansNodeId", nodeId), null, "jansNum", SortOrder.DESCENDING, 1, 1, 1);
//		if (pagedResult.getEntriesCount() >= 1) {
//			return setIndexes(pagedResult.getEntries().get(0));
//		}
//
//		return null;
    }

    /**
     * Gets pool by global status list index
     *
     * @param index status list index
     * @return token pool
     */
    public StatusIndexPool getPoolByIndex(int index) {
        int poolId = index / indexAllocationBlockSize;

        return getPoolById(poolId);
    }

    /**
     * Returns a list of all StatusIndexPools associated with ClusterNode
     *
     * @return list of TokenPools
     */
    public List<StatusIndexPool> getNodePools(Integer nodeId) {
        String baseDn = baseDn();

        return setIndexes(entryManager.findEntries(baseDn, StatusIndexPool.class, Filter.createEqualityFilter("jansNodeId", nodeId)));
    }

    /**
     * Returns a list of expired pools
     *
     * @return list of pools
     */
    public List<StatusIndexPool> getPoolsExpired(int nodeId) {
        final String baseDn = baseDn();

        Filter expFilter = Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(baseDn, new Date()));
        Filter nodeFilter = Filter.createEqualityFilter("jansNodeId", nodeId);
        Filter filter = Filter.createANDFilter(nodeFilter, expFilter);
        return setIndexes(entryManager.findEntries(baseDn, StatusIndexPool.class, filter));
    }

    protected void persist(StatusIndexPool pool) {
        entryManager.persist(pool);
    }

    public void update(StatusIndexPool pool) {
        entryManager.merge(pool);
    }

    public StatusIndexPool updateWithLock(StatusIndexPool pool) {
        log.debug("Attempt to update pool {} with lock {}...", pool.getId(), LOCK_KEY);

        int attempt = 1;
        do {
            StatusIndexPool loadedPool = getPoolByDn(pool.getDn());

            loadedPool.setLockKey(LOCK_KEY);
            loadedPool.setData(pool.getData());
            loadedPool.setLastUpdate(new Date());
            loadedPool.setExpirationDate(pool.getExpirationDate());

            update(loadedPool);

            // reload and check lock
            loadedPool = getPoolByDn(loadedPool.getDn());

            // if lock is ours do data update and release lock
            if (LOCK_KEY.equals(loadedPool.getLockKey())) {
                log.debug("Updated pool {} with lock with attempt {}", loadedPool.getId(), LOCK_KEY, attempt);
                return loadedPool;
            } else {
                log.debug("Failed to update pool {} with lock {} with attempt {}", loadedPool.getId(), LOCK_KEY, attempt);
            }

            attempt++;
        } while (attempt < ATTEMPT_LIMIT);

        log.error("Unable to update pool {} with lock {} with attempt {}", pool.getId(), LOCK_KEY, attempt);

        return null;
    }

    public StatusIndexPool allocate(int nodeId) {
        // Try to use existing expired entry
        List<StatusIndexPool> expiredPools = getPoolsExpired(nodeId);

        // expiration date of the pool is double of access token lifetime
        Date expirationDate = new Date(System.currentTimeMillis() + 2 * appConfiguration.getAccessTokenLifetime() * 1000);

        for (StatusIndexPool pool : expiredPools) {
            // Do lock operation in try/catch for safety and do not throw error to upper levels
            try {
                pool.setLockKey(LOCK_KEY);
                pool.setExpirationDate(expirationDate);
                pool.setLastUpdate(new Date());

                update(pool);

                // Load token after update
                StatusIndexPool lockedTokenPool = getPoolByDn(pool.getDn());

                // If lock is ours reset entry and return it
                if (LOCK_KEY.equals(lockedTokenPool.getLockKey())) {
                    return lockedTokenPool;
                }
            } catch (EntryPersistenceException ex) {
                log.trace("Unexpected error happened during entry lock", ex);
            }
        }

        // There are no free entries. server need to add new one with next index
        int attempt = 1;
        do {
            log.debug("Attempting to persist new status index pool. Attempt {} out of {}", attempt, ATTEMPT_LIMIT);

            StatusIndexPool lastTokenPool = getPoolLast();

            int lastTokenPoolIndex = lastTokenPool == null ? 0 : lastTokenPool.getId() + 1;

            StatusIndexPool pool = new StatusIndexPool();
            pool.setId(lastTokenPoolIndex);
            pool.setDn(createDn(lastTokenPoolIndex));
            pool.setNodeId(nodeId);
            pool.setLastUpdate(new Date());
            pool.setLockKey(LOCK_KEY);
            pool.setExpirationDate(expirationDate);

            // Do persist operation in try/catch for safety and do not throw error to upper
            // levels
            try {
                persist(pool);

                // Load token after update
                StatusIndexPool lockedTokenPool = getPoolByDn(pool.getDn());

                // if lock is ours return it
                if (LOCK_KEY.equals(lockedTokenPool.getLockKey())) {
                    log.debug("Successfully created new status index pool {}", lockedTokenPool.getId());
                    return setIndexes(lockedTokenPool);
                } else {
                    log.debug("Failed to create new status index pool {}", lockedTokenPool.getId());
                }
            } catch (EntryPersistenceException ex) {
                log.trace("Unexpected error happened during entry lock", ex);
            }
            attempt++;
        } while (attempt <= ATTEMPT_LIMIT);

        // This should not happens
        throw new EntryPersistenceException("Failed to allocate StatusIndexPool!!!");
    }

    private StatusIndexPool setIndexes(StatusIndexPool pool) {
        if (pool == null) {
            return pool;
        }

        int index = pool.getId();

        pool.setStartIndex(index * indexAllocationBlockSize);
        pool.setEndIndex((index + 1) * indexAllocationBlockSize - 1);

        return pool;
    }

    private List<StatusIndexPool> setIndexes(List<StatusIndexPool> pools) {
        if (pools == null) {
            return pools;
        }

        for (StatusIndexPool pool : pools) {
            setIndexes(pool);
        }
        return pools;
    }

    public String baseDn() {
        return staticConfiguration.getBaseDn().getStatusIndexPool();
    }

    public String createDn(int id) {
        String baseDn = baseDn();
        return String.format("jansNum=%d,%s", id, baseDn);
    }
}
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
import io.jans.model.tokenstatus.StatusList;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.service.cdi.util.CdiUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static io.jans.as.server.service.cluster.ClusterNodeService.LOCK_KEY;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class StatusIndexPoolService {

    public static final int ATTEMPT_LIMIT = 10;
    public static long DELAY_AFTER_EXPIRATION = 3 * 60 * 60 * 1000L; // 3 hours
    public static long LOCK_WAIT_BEFORE_UPDATE = 3 * 1000L; // 30 seconds
    public static long DELAY_IF_LOCKED = 500; // 50 milliseconds
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

    public static StatusIndexPool setIndexes(StatusIndexPool pool, int indexAllocationBlockSize) {
        if (pool == null) {
            return pool;
        }

        int index = pool.getId();

        pool.setStartIndex(index * indexAllocationBlockSize);
        pool.setEndIndex((index + 1) * indexAllocationBlockSize - 1);

        return pool;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Status Index Pool Service ...");
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
        String baseDn = baseDn();

        int count = 1;
        if (PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(entryManager.getPersistenceType(baseDn))) {
            count = Integer.MAX_VALUE;
        }

        PagedResult<StatusIndexPool> pagedResult = entryManager.findPagedEntries(baseDn, StatusIndexPool.class,
                Filter.createPresenceFilter("jansNum"), null, "jansNum", SortOrder.DESCENDING, 0, count, count);
        if (pagedResult.getEntriesCount() >= 1) {
            return setIndexes(pagedResult.getEntries().get(0));
        }

        return null;
    }

    /**
     * Gets pool by global status list index
     *
     * @param index status list index
     * @return pool
     */
    public StatusIndexPool getPoolByIndex(int index) {
        int poolId = index / indexAllocationBlockSize;

        return getPoolById(poolId);
    }

    /**
     * Returns a list of all StatusIndexPools associated with ClusterNode
     *
     * @return list of pools
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
    public List<StatusIndexPool> getPoolsExpired() {
        final String baseDn = baseDn();

        Date expirationDate = new Date(System.currentTimeMillis() - DELAY_AFTER_EXPIRATION);

        Filter filter = Filter.createORFilter(Filter.createEqualityFilter("exp", null),
                Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(baseDn, expirationDate)));

        return setIndexes(entryManager.findEntries(baseDn, StatusIndexPool.class, filter));
    }

    protected void persist(StatusIndexPool pool) {
        entryManager.persist(pool);
    }

    public void update(StatusIndexPool pool) {
        entryManager.merge(pool);
    }

    public StatusIndexPool updateWithLock(String poolDn, List<Integer> indexes, TokenStatus status) throws IOException {
        log.debug("Attempt to update pool {} with lock {}...", poolDn, LOCK_KEY);

        int attempt = 1;
        do {
            StatusIndexPool loadedPool = getPoolByDn(poolDn);

            int bitSize = appConfiguration.getStatusListBitSize();
            StatusList statusList = StringUtils.isNotBlank(loadedPool.getData()) ? StatusList.fromEncoded(loadedPool.getData(), bitSize) : new StatusList(bitSize);
            for (Integer index : indexes) {
                statusList.set(index, status.getValue());
            }

            loadedPool.setLockKey(LOCK_KEY);
            loadedPool.setData(statusList.getLst());
            loadedPool.setLastUpdate(new Date());
            loadedPool.setExpirationDate(loadedPool.getExpirationDate());

            update(loadedPool);

            // reload and check lock
            loadedPool = getPoolByDn(loadedPool.getDn());

            // if lock is ours do data update and release lock
            if (LOCK_KEY.equals(loadedPool.getLockKey())) {
                log.debug("Updated pool {} with lock with attempt {}, lockKey: {}", loadedPool.getId(), attempt, LOCK_KEY);
                return loadedPool;
            } else {
                log.debug("Failed to update pool {} with lock {} with attempt {}", loadedPool.getId(), LOCK_KEY, attempt);
            }

            attempt++;
        } while (attempt < ATTEMPT_LIMIT);

        log.error("Unable to update pool {} with lock {} with attempt {}", poolDn, LOCK_KEY, attempt);

        return null;
    }

    public StatusIndexPool allocate(int nodeId) {
        log.debug("Allocating status index pool, node {}, LOCK_KEY {}... ", nodeId, LOCK_KEY);

        // Try to use existing expired entry
        List<StatusIndexPool> expiredPools = getPoolsExpired();

        log.debug("Allocation - found {} expired status index pools, node {}.", expiredPools.size(), nodeId);

        // expiration date of the pool is double of access token lifetime
        Date expirationDate = new Date(System.currentTimeMillis() + 2 * appConfiguration.getAccessTokenLifetime() * 1000);

        for (StatusIndexPool pool : expiredPools) {
            // Do lock operation in try/catch for safety and do not throw error to upper levels
            try {
                pool.setLockKey(LOCK_KEY);
                pool.setExpirationDate(expirationDate);
                pool.setLastUpdate(new Date());
                pool.setNodeId(nodeId);

                update(pool);

                // Load pool after update
                StatusIndexPool lockedPool = getPoolByDn(pool.getDn());

                // If lock is ours reset entry and return it
                if (LOCK_KEY.equals(lockedPool.getLockKey()) && lockedPool.getNodeId().equals(nodeId)) {
                    log.debug("Re-using existing status index pool {}, node {}, LOCK_KEY {}", lockedPool.getId(), nodeId, LOCK_KEY);

                    // mark all indexes which we are re-using as VALID
                    StatusListIndexService indexService = CdiUtil.bean(StatusListIndexService.class);
                    indexService.updateStatusAtIndexes(lockedPool.enumerateAllIndexes(), TokenStatus.VALID);
                    return lockedPool;
                }
            } catch (EntryPersistenceException ex) {
                log.debug("Unexpected error happened during entry lock, node " + nodeId, ex);
            }
        }

        // There are no free entries. server need to add new one with next index
        int attempt = 1;
        do {
            log.debug("Attempting to persist new status index pool. Attempt {} out of {}. Node {}.", attempt, ATTEMPT_LIMIT, nodeId);

            StatusIndexPool lastPool = getPoolLast();

            int lastPoolIndex = lastPool == null ? 0 : lastPool.getId() + 1;

            StatusIndexPool pool = new StatusIndexPool();
            pool.setId(lastPoolIndex);
            pool.setDn(createDn(lastPoolIndex));
            pool.setNodeId(nodeId);
            pool.setLastUpdate(new Date());
            pool.setLockKey(LOCK_KEY);

            // Do persist operation in try/catch for safety and do not throw error to upper
            // levels
            try {
                expirationDate = new Date(System.currentTimeMillis() + 2 * appConfiguration.getAccessTokenLifetime() * 1000);
                pool.setExpirationDate(expirationDate);

                try {
                    log.debug("Trying to persist index {} for node {}", lastPoolIndex, nodeId);

                    persist(pool);
                } catch (EntryPersistenceException e) {
                    if (e.getCause() instanceof DuplicateEntryException) {
                        lastPoolIndex = lastPoolIndex + 1;
                        log.debug("Detected duplicate entry, increased index to {}", lastPoolIndex);

                        pool.setId(lastPoolIndex);
                        pool.setDn(createDn(lastPoolIndex));

                        persist(pool);
                    } else {
                        throw e;
                    }
                }

                // Load pool after update
                StatusIndexPool lockedPool = getPoolByDn(pool.getDn());

                // if lock is ours return it
                if (LOCK_KEY.equals(lockedPool.getLockKey()) && lockedPool.getNodeId().equals(nodeId)) {
                    log.debug("Successfully created new status index pool {}, node {}", lockedPool.getId(), nodeId);
                    return setIndexes(lockedPool);
                } else {
                    log.debug("Failed to create new status index pool {}, node {}", lockedPool.getId(), nodeId);
                }
            } catch (EntryPersistenceException ex) {
                log.debug("Unexpected error happened during entry lock, node " + nodeId, ex);
            }
            attempt++;
        } while (attempt <= ATTEMPT_LIMIT);

        // This should not happens
        throw new EntryPersistenceException(String.format("Failed to allocate StatusIndexPool for node %s!!!", nodeId));
    }

    private StatusIndexPool setIndexes(StatusIndexPool pool) {
        return setIndexes(pool, indexAllocationBlockSize);
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
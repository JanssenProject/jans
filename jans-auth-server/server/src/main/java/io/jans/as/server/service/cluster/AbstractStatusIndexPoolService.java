/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.token.AbstractIndexPool;
import io.jans.model.tokenstatus.StatusList;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import static io.jans.as.server.service.cluster.ClusterNodeService.LOCK_KEY;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
public abstract class AbstractStatusIndexPoolService<T extends AbstractIndexPool> {

    public static final int ATTEMPT_LIMIT = 10;
    public static long DELAY_AFTER_EXPIRATION = 3 * 60 * 60 * 1000L; // 3 hours
    public static long LOCK_WAIT_BEFORE_UPDATE = 3 * 1000L; // 30 seconds
    public static long DELAY_IF_LOCKED = 500; // 50 milliseconds

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private PersistenceEntryManager entryManager;

    // Don't allow to change it after server start up. After setting new value we need to restart cluster
    private int indexAllocationBlockSize;

    public static AbstractIndexPool setIndexes(AbstractIndexPool pool, int indexAllocationBlockSize) {
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
        final String logPrefix = logPrefix();
        log.info("{} Initializing Status Index Pool Service ...", logPrefix);
        indexAllocationBlockSize = appConfiguration.getStatusListIndexAllocationBlockSize();
    }

    /**
     * Returns pool by dn
     *
     * @return pool
     */
    public T getPoolByDn(String dn) {
        return setIndexes(entryManager.find(getEntityClass(), dn));
    }

    /**
     * Returns pool by Id
     *
     * @return pool
     */
    public T getPoolById(int id) {
        return getPoolByDn(createDn(id));
    }

    /**
     * Returns a list of all pools
     *
     * @return list of pools
     */
    public List<T> getAllPools() {
        return setIndexes(entryManager.findEntries(baseDn(), getEntityClass(), Filter.createPresenceFilter("jansNum")));
    }

    /**
     * Returns last (max) pool or null if none
     *
     * @return pool
     */
    public T getPoolLast() {
        String baseDn = baseDn();

        int count = 1;
        if (PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(entryManager.getPersistenceType(baseDn))) {
            count = Integer.MAX_VALUE;
        }

        PagedResult<T> pagedResult = entryManager.findPagedEntries(baseDn, getEntityClass(),
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
    public T getPoolByIndex(int index) {
        int poolId = index / indexAllocationBlockSize;

        return getPoolById(poolId);
    }

    /**
     * Returns a list of all pools associated with ClusterNode
     *
     * @return list of pools
     */
    public List<T> getNodePools(Integer nodeId) {
        String baseDn = baseDn();

        return setIndexes(entryManager.findEntries(baseDn, getEntityClass(), Filter.createEqualityFilter("jansNodeId", nodeId)));
    }

    /**
     * Returns a list of expired pools
     *
     * @return list of pools
     */
    public List<T> getPoolsExpired() {
        final String baseDn = baseDn();

        Date expirationDate = new Date(System.currentTimeMillis() - DELAY_AFTER_EXPIRATION);

        Filter filter = Filter.createORFilter(Filter.createEqualityFilter("exp", null),
                Filter.createLessOrEqualFilter("exp", entryManager.encodeTime(baseDn, expirationDate)));

        return setIndexes(entryManager.findEntries(baseDn, getEntityClass(), filter));
    }

    protected void persist(T pool) {
        entryManager.persist(pool);
    }

    public void update(T pool) {
        entryManager.merge(pool);
    }

    public T updateWithLock(String poolDn, List<Integer> indexes, TokenStatus status) throws IOException {
        final String logPrefix = logPrefix();
        log.debug("{} Attempt to update pool {} with lock {}...", logPrefix, poolDn, LOCK_KEY);

        int attempt = 1;
        do {
            T loadedPool = getPoolByDn(poolDn);

            int bitSize = getStatusListBitSize();
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
                log.debug("{} Updated pool {} with lock with attempt {}, lockKey: {}", logPrefix, loadedPool.getId(), attempt, LOCK_KEY);
                return loadedPool;
            } else {
                log.debug("{} Failed to update pool {} with lock {} with attempt {}", logPrefix, loadedPool.getId(), LOCK_KEY, attempt);
            }

            attempt++;
        } while (attempt < ATTEMPT_LIMIT);

        log.error("{} Unable to update pool {} with lock {} with attempt {}", logPrefix, poolDn, LOCK_KEY, attempt);

        return null;
    }

    public T allocate(int nodeId) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final String logPrefix = logPrefix();
        log.debug("{} Allocating status index pool, node {}, LOCK_KEY {}... ", logPrefix, nodeId, LOCK_KEY);

        // Try to use existing expired entry
        List<T> expiredPools = getPoolsExpired();

        log.debug("{} Allocation - found {} expired status index pools, node {}.", logPrefix, expiredPools.size(), nodeId);

        // expiration date of the pool is double of access token lifetime
        Date expirationDate = new Date(System.currentTimeMillis() + 2 * appConfiguration.getAccessTokenLifetime() * 1000);

        for (T pool : expiredPools) {
            // Do lock operation in try/catch for safety and do not throw error to upper levels
            try {
                pool.setLockKey(LOCK_KEY);
                pool.setExpirationDate(expirationDate);
                pool.setLastUpdate(new Date());
                pool.setNodeId(nodeId);

                update(pool);

                // Load pool after update
                T lockedPool = getPoolByDn(pool.getDn());

                // If lock is ours reset entry and return it
                if (LOCK_KEY.equals(lockedPool.getLockKey()) && lockedPool.getNodeId().equals(nodeId)) {
                    log.debug("{} Re-using existing status index pool {}, node {}, LOCK_KEY {}", logPrefix, lockedPool.getId(), nodeId, LOCK_KEY);

                    markAllIndexesAsValid(lockedPool.enumerateAllIndexes());
                    return lockedPool;
                }
            } catch (EntryPersistenceException ex) {
                log.debug(logPrefix + " Unexpected error happened during entry lock, node " + nodeId, ex);
            }
        }

        // There are no free entries. server need to add new one with next index
        int attempt = 1;
        do {
            log.debug("{} Attempting to persist new status index pool. Attempt {} out of {}. Node {}.", logPrefix, attempt, ATTEMPT_LIMIT, nodeId);

            T lastPool = getPoolLast();

            int lastPoolIndex = lastPool == null ? 0 : lastPool.getId() + 1;

                Class<T> entityClass = getEntityClass();

            // Creates a new instance using the no-arg constructor
            T pool = entityClass.getDeclaredConstructor().newInstance();
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
                    log.debug("{} Trying to persist index {} for node {}", logPrefix, lastPoolIndex, nodeId);

                    persist(pool);
                } catch (EntryPersistenceException e) {
                    if (e.getCause() instanceof DuplicateEntryException) {
                        lastPoolIndex = lastPoolIndex + 1;
                        log.debug("{} Detected duplicate entry, increased index to {}", logPrefix, lastPoolIndex);

                        pool.setId(lastPoolIndex);
                        pool.setDn(createDn(lastPoolIndex));

                        persist(pool);
                    } else {
                        throw e;
                    }
                }

                // Load pool after update
                T lockedPool = getPoolByDn(pool.getDn());

                // if lock is ours return it
                if (LOCK_KEY.equals(lockedPool.getLockKey()) && lockedPool.getNodeId().equals(nodeId)) {
                    log.debug("{} Successfully created new status index pool {}, node {}", logPrefix, lockedPool.getId(), nodeId);
                    return setIndexes(lockedPool);
                } else {
                    log.debug("{} Failed to create new status index pool {}, node {}", logPrefix, lockedPool.getId(), nodeId);
                }
            } catch (EntryPersistenceException ex) {
                log.debug(logPrefix + " Unexpected error happened during entry lock, node " + nodeId, ex);
            }
            attempt++;
        } while (attempt <= ATTEMPT_LIMIT);

        // This should not happens
        throw new EntryPersistenceException(String.format("%s Failed to allocate StatusIndexPool for node %s!!!", logPrefix, nodeId));
    }

    private T setIndexes(T pool) {
        return (T) setIndexes(pool, indexAllocationBlockSize);
    }

    private List<T> setIndexes(List<T> pools) {
        if (pools == null) {
            return pools;
        }

        for (T pool : pools) {
            setIndexes(pool);
        }
        return pools;
    }

    public abstract String baseDn();

    public abstract int getStatusListBitSize();

    public abstract Class<T> getEntityClass();

    public abstract void markAllIndexesAsValid(List<Integer> enumerateAllIndexes);

    public abstract String logPrefix();

    public String createDn(int id) {
        String baseDn = baseDn();
        return String.format("jansNum=%d,%s", id, baseDn);
    }
}
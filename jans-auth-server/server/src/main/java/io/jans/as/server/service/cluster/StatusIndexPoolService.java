/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import static io.jans.as.server.service.cluster.ClusterNodeService.LOCK_KEY;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.token.StatusIndexPool;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
    	String baseDn = staticConfiguration.getBaseDn().getNode();
    	PagedResult<StatusIndexPool> pagedResult = entryManager.findPagedEntries(baseDn, StatusIndexPool.class, Filter.createPresenceFilter("jansNum"), null, "jansNum", SortOrder.DESCENDING, 0, 1, 1);
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

                update(pool);

                // Load pool after update
                StatusIndexPool lockedPool = getPoolByDn(pool.getDn());

                // If lock is ours reset entry and return it
                if (LOCK_KEY.equals(lockedPool.getLockKey())) {
                	// Assign record for specific nodeId 
                	lockedPool.setNodeId(nodeId);

                    update(lockedPool);

                    log.debug("Re-using existing status index pool {}, node {}, LOCK_KEY {}", lockedPool.getId(), nodeId, LOCK_KEY);
                    return lockedPool;
                }
            } catch (EntryPersistenceException ex) {
                log.trace("Unexpected error happened during entry lock, node " + nodeId, ex);
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

                persist(pool);

                // Load pool after update
                StatusIndexPool lockedPool = getPoolByDn(pool.getDn());

                // if lock is ours return it
                if (LOCK_KEY.equals(lockedPool.getLockKey())) {
                    log.debug("Successfully created new status index pool {}, node {}", lockedPool.getId(), nodeId);
                    return setIndexes(lockedPool);
                } else {
                    log.debug("Failed to create new status index pool {}, node {}", lockedPool.getId(), nodeId);
                }
            } catch (EntryPersistenceException ex) {
                log.trace("Unexpected error happened during entry lock, node " + nodeId, ex);
            }
            attempt++;
        } while (attempt <= ATTEMPT_LIMIT);

        // This should not happens
        throw new EntryPersistenceException(String.format("Failed to allocate StatusIndexPool for node %s!!!", nodeId));
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
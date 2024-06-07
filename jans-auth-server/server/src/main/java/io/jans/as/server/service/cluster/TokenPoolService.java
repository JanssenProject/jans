/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.model.token.TokenPool;
import io.jans.model.token.TokenPoolStatus;
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
public class TokenPoolService {

	@Inject
	private Logger log;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private PersistenceEntryManager entryManager;

	// Don't allow to change it after server start up. After setting new value we need to restart cluster
	private int tokenIndexAllocationBlockSize;

	@PostConstruct
	public void init() {
		log.info("Initializing Token Pool Service ...");
		tokenIndexAllocationBlockSize = appConfiguration.getTokenIndexAllocationBlockSize();
	}

	/**
	 * returns TokenPool by Dn
	 *
	 * @return TokenPool
	 */
	public TokenPool getTokenPoolByDn(String dn) {
		return setIndexes(entryManager.find(TokenPool.class, dn));
	}

	/**
	 * returns TokenPool by Id
	 *
	 * @return TokenPool
	 */
	public TokenPool getTokenPoolById(Integer id) {
		return setIndexes(entryManager.find(TokenPool.class, getDnForTokenPool(id)));
	}

	/**
	 * returns a list of all TokenPools
	 *
	 * @return list of TokenPools
	 */
	public List<TokenPool> getAllTokenPools() {
		String tokenPoolsBaseDn = staticConfiguration.getBaseDn().getNodes();

		return setIndexes(entryManager.findEntries(tokenPoolsBaseDn, TokenPool.class, Filter.createPresenceFilter("jansNum")));
	}

	/**
	 * returns a list of TokenPools with specific status
	 *
	 * @return list of TokenPools
	 */
	public List<TokenPool> getTokenPools(TokenPoolStatus status) {
		String tokenPoolsBaseDn = staticConfiguration.getBaseDn().getNodes();

		return setIndexes(entryManager.findEntries(tokenPoolsBaseDn, TokenPool.class, Filter.createEqualityFilter("tokenStatus", status)));
	}

	/**
	 * returns last TokenPool or null if none
	 *
	 * @return TokenPool
	 */
	public TokenPool getTokenPoolLast() {
		String tokenPoolsBaseDn = staticConfiguration.getBaseDn().getNodes();
		
		PagedResult<TokenPool> pagedResult = entryManager.findPagedEntries(tokenPoolsBaseDn, TokenPool.class, Filter.createPresenceFilter("jansNum"), null, "jansNum", SortOrder.DESCENDING, 1, 1, 1);
		if (pagedResult.getEntriesCount() >= 1) {
			return setIndexes(pagedResult.getEntries().get(0));
		}
		

		return null;
	}

    /**
     * Gets token pool by global status list index
     *
     * @param index global status list index
     * @return token pool
     */
	public TokenPool getTokenPoolByIndex(int index) {
		int poolId = index / tokenIndexAllocationBlockSize;
		
		return getTokenPoolById(poolId);
    }

	/**
	 * returns a list of all TokenPools associated with CluterNode
	 *
	 * @return list of TokenPools
	 */
	public List<TokenPool> getClusterNodeTokenPools(Integer clusterNodeId) {
		String tokenPoolsBaseDn = staticConfiguration.getBaseDn().getNodes();

		return setIndexes(entryManager.findEntries(tokenPoolsBaseDn, TokenPool.class, Filter.createEqualityFilter("jansNodeId", clusterNodeId)));
	}

	public List<String> getTokenPoolsDns(List<Integer> nodeIds) {
		List<String> tokenPoolsDns = new ArrayList<>();

		for (Integer nodeId : nodeIds) {
			TokenPool clusterNode = getTokenPoolById(nodeId);
			if (clusterNode != null) {
				tokenPoolsDns.add(clusterNode.getDn());
			}
		}

		return tokenPoolsDns;
	}
	
	public TokenPool allocate(Integer nodeId) {
		// Try to use existing entry
		List<TokenPool> tokenPools = getTokenPools(TokenPoolStatus.FREE);
		
		for (TokenPool tokenPool : tokenPools) {
			// Attempt to set random value in lockKey
			String lockKey = UUID.randomUUID().toString();
			tokenPool.setLockKey(lockKey);
			
			// Do lock operation in try/catch for safety and do not throw error to upper levels 
			try {
				update(tokenPool);
				
				// Load token after update
				TokenPool lockedTokenPool = getTokenPoolByDn(tokenPool.getDn());
				
				// if lock is ours reset entry and return it
				if (lockKey.equals(lockedTokenPool.getLockKey())) {
					reset(tokenPool, nodeId);
				}
				
				return tokenPool;
			} catch (EntryPersistenceException ex) {
				log.trace("Unexpected error happened during entry lock", ex);
			}
		}
		
		// There are no free entries. server need to add new one with next index
		int maxSteps  = 10;
		do {
			TokenPool lastTokenPool = getTokenPoolLast();

			Integer lastTokenPoolIndex = lastTokenPool == null ? 0 : lastTokenPool.getId() + 1;

			TokenPool tokenPool = new TokenPool();
			tokenPool.setId(lastTokenPoolIndex);
			tokenPool.setDn(getDnForTokenPool(lastTokenPoolIndex));
			tokenPool.setNodeId(nodeId);
			tokenPool.setLastUpdate(new Date());
			tokenPool.setStatus(TokenPoolStatus.INUSE);

			// Attempt to set random value in lockKey
			String lockKey = UUID.randomUUID().toString();
			tokenPool.setLockKey(lockKey);

			// Do persist ooperation in try/catch for safety and do not throw error to upper
			// levels
			try {
				persist(lastTokenPool);

				// Load token after update
				TokenPool lockedTokenPool = getTokenPoolByDn(tokenPool.getDn());

				// if lock is ours return it
				if (lockKey.equals(lockedTokenPool.getLockKey())) {
					return tokenPool;
				}

			} catch (EntryPersistenceException ex) {
				log.trace("Unexpected error happened during entry lock", ex);
			}
			log.debug("Attempting to persist new token list. Attempt before fail: '{}'", maxSteps);
		} while (maxSteps >= 0);		

		// This should not happens
		throw new EntryPersistenceException("Failed to allocate TokenPool!!!");
	}

	public void release(TokenPool tokenPool) {
		tokenPool.setData(null);
		tokenPool.setLastUpdate(null);
		tokenPool.setNodeId(null);
		tokenPool.setLockKey(null);
		
		update(tokenPool);
	}

	protected void persist(TokenPool tokenPool) {
		entryManager.persist(tokenPool);
	}

	public void reset(TokenPool tokenPool, Integer nodeId) {
		tokenPool.setNodeId(nodeId);
		tokenPool.setData(null);
		tokenPool.setLastUpdate(new Date());
		
		update(tokenPool);
	}

	public void update(TokenPool tokenPool) {
		entryManager.merge(tokenPool);
	}

	private TokenPool setIndexes(TokenPool tokenPool) {
		if (tokenPool == null) {
			return tokenPool;
		}

		int tokenPoolIndex = tokenPool.getId();
		
		tokenPool.setStartIndex(tokenPoolIndex * tokenIndexAllocationBlockSize);
		tokenPool.setEndIndex((tokenPoolIndex + 1) * tokenIndexAllocationBlockSize - 1);

		return tokenPool;
	}

	private List<TokenPool> setIndexes(List<TokenPool> tokenPools) {
		if (tokenPools == null) {
			return tokenPools;
		}

		for (TokenPool tokenPool : tokenPools) {
			setIndexes(tokenPool);
		}
		return tokenPools;
	}

	public String getDnForTokenPool(Integer id) {
		return String.format("jansNum=%d,%s", id, staticConfiguration.getBaseDn().getNodes());
	}

}
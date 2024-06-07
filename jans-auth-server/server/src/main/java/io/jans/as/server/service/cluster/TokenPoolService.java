/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.model.token.TokenPool;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
	private PersistenceEntryManager entryManager;

	/**
	 * returns TokenPool by Dn
	 *
	 * @return TokenPool
	 */
	public TokenPool getTokenPoolByDn(String dn) {
		return entryManager.find(TokenPool.class, dn);
	}

	/**
	 * returns TokenPool by Id
	 *
	 * @return TokenPool
	 */
	public TokenPool getTokenPoolById(Integer id) {
		return entryManager.find(TokenPool.class, getDnForTokenPool(id));
	}

	/**
	 * returns a list of all TokenPools
	 *
	 * @return list of TokenPools
	 */
	public List<TokenPool> getAllTokenPools() {
		String tokenPoolsBaseDn = staticConfiguration.getBaseDn().getNodes();

		return entryManager.findEntries(tokenPoolsBaseDn, TokenPool.class, Filter.createPresenceFilter("jansNum"));
	}

    /**
     * Gets token pool by global status list index
     *
     * @param index global status list index
     * @return token pool
     */
	public TokenPool getTokenPoolByIndex(int index) {
	    throw new UnsupportedOperationException("WIP"); // todo
    }

	/**
	 * returns a list of all TokenPools associated with CluterNode
	 *
	 * @return list of TokenPools
	 */
	public List<TokenPool> getClusterNodeTokenPools(Integer clusterNodeId) {
		String tokenPoolsBaseDn = staticConfiguration.getBaseDn().getNodes();

		return entryManager.findEntries(tokenPoolsBaseDn, TokenPool.class, Filter.createEqualityFilter("jansNodeId", clusterNodeId));
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

	public void persist(TokenPool tokenPool) {
		entryManager.persist(tokenPool);
	}

	public void update(TokenPool tokenPool) {
		entryManager.merge(tokenPool);
	}

	public String getDnForTokenPool(Integer id) {
		return String.format("jansNum=%d,%s", id, staticConfiguration.getBaseDn().getNodes());
	}

}
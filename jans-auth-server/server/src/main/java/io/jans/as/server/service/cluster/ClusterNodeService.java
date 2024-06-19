/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.exception.ConfigurationException;
import io.jans.model.cluster.ClusterNode;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.tika.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
@ApplicationScoped
public class ClusterNodeService {

    public static final int ATTEMPT_LIMIT = 10;

    public static final long DELAY_AFTER_EXPIRATION = 3 * 60 * 1000L; // 3 minutes
    public static final String CLUSTER_TYPE_JANS_AUTH = "jans-auth";
    public static final String JANS_TYPE_ATTR_NAME = "jansType";

    public static final String lockKey = UUID.randomUUID().toString();

	@Inject
	private Logger log;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private PersistenceEntryManager entryManager;

	/**
	 * returns ClusterNode by Dn
	 *
	 * @return ClusterNode
	 */
	public ClusterNode getClusterNodeByDn(String dn) {
		return entryManager.find(ClusterNode.class, dn);
	}

	/**
	 * returns ClusterNode by Id
	 *
	 * @return ClusterNode
	 */
	public ClusterNode getClusterNodeById(Integer id) {
		return entryManager.find(ClusterNode.class, getDnForClusterNode(id));
	}

	/**
	 * returns a list of all ClusterNodes
	 *
	 * @return list of ClusterNodes
	 */
	public List<ClusterNode> getAllClusterNodes() {
		String clusterNodesBaseDn = staticConfiguration.getBaseDn().getNode();

        return entryManager.findEntries(clusterNodesBaseDn, ClusterNode.class, getTypeFilter());
    }

	public List<String> getClusterNodesDns(List<Integer> nodeIds) {
		List<String> clusterNodesDns = new ArrayList<>();

		for (Integer nodeId : nodeIds) {
			ClusterNode clusterNode = getClusterNodeById(nodeId);
			if (clusterNode != null) {
				clusterNodesDns.add(clusterNode.getDn());
			}
		}

		return clusterNodesDns;
	}

	/**
	 * returns last TokenPool or null if none
	 *
	 * @return TokenPool
	 */
	public ClusterNode getClusterNodeLast() {
		String clusterNodesBaseDn = staticConfiguration.getBaseDn().getNode();

        PagedResult<ClusterNode> pagedResult = entryManager.findPagedEntries(clusterNodesBaseDn, ClusterNode.class, getTypeFilter(), null, "jansNum", SortOrder.DESCENDING, 0, 1, 1);
        if (pagedResult.getEntriesCount() >= 1) {
            return pagedResult.getEntries().get(0);
        }

		return null;
	}

	/**
	 * returns a list of expired ClusterNodes
	 *
	 * @return list of ClusterNodes
	 */
	public List<ClusterNode> getClusterNodesExpired() {
		String clusterNodesBaseDn = staticConfiguration.getBaseDn().getNode();
		if (StringUtils.isBlank(clusterNodesBaseDn)) {
            throw new ConfigurationException("ou=node is not configured in static configuration of AS (jansConfStatic).");
        }

        Date expirationDate = new Date(System.currentTimeMillis() + DELAY_AFTER_EXPIRATION);

        Filter filter = Filter.createANDFilter(getTypeFilter(),
                Filter.createGreaterOrEqualFilter("jansLastUpd", entryManager.encodeTime(clusterNodesBaseDn, expirationDate)));

		return entryManager.findEntries(clusterNodesBaseDn, ClusterNode.class, filter);
	}

    @NotNull
    private Filter getTypeFilter() {
        return Filter.createEqualityFilter(JANS_TYPE_ATTR_NAME, CLUSTER_TYPE_JANS_AUTH);
    }

	protected void persist(ClusterNode clusterNode) {
		entryManager.persist(clusterNode);
	}

	public void update(ClusterNode clusterNode) {
		entryManager.merge(clusterNode);
	}

	public ClusterNode allocate() {
        log.debug("Allocation, lockKey {}... ", lockKey);

		// Try to use existing expired entry (node is expired if not used for 3 minutes)
		List<ClusterNode> expiredNodes = getClusterNodesExpired();
        log.debug("Allocation - found {} expired nodes.", expiredNodes.size());

		for (ClusterNode expiredNode : expiredNodes) {
			// Do lock operation in try/catch for safety and do not throw error to upper levels
			try {
                Date currentTime = new Date();

                expiredNode.setCreationDate(currentTime);
                expiredNode.setLastUpdate(currentTime);
                expiredNode.setLockKey(lockKey);

				update(expiredNode);

				// Load node after update
				ClusterNode lockedNode = getClusterNodeByDn(expiredNode.getDn());

				// If lock is ours reset entry and return it
				if (lockKey.equals(lockedNode.getLockKey())) {
				    log.debug("Re-using existing node {}, lockKey {}", lockedNode.getId(), lockKey);
					return lockedNode;
				}

                log.debug("Failed to lock node {}, lockKey {}", lockedNode.getId(), lockKey);
			} catch (EntryPersistenceException ex) {
				log.debug("Unexpected error happened during entry lock", ex);
			}
		}

		// There are no free entries. server need to add new one with next index
		int attempt  = 1;
		do {
            log.debug("Attempting to persist new node. Attempt {} out of {} ...", attempt, ATTEMPT_LIMIT);

			ClusterNode lastClusterNode = getClusterNodeLast();
			log.debug("lastClusterNode - {}, lockKey {}", lastClusterNode != null ? lastClusterNode.getId() : -1, lockKey);

			Integer lastClusterNodeIndex = lastClusterNode == null ? 0 : lastClusterNode.getId() + 1;

			Date currentTime = new Date();

			ClusterNode node = new ClusterNode();
			node.setId(lastClusterNodeIndex);
			node.setDn(getDnForClusterNode(lastClusterNodeIndex));
			node.setCreationDate(currentTime);
			node.setLastUpdate(currentTime);
			node.setType(CLUSTER_TYPE_JANS_AUTH);
			node.setLockKey(lockKey);

			// Do persist operation in try/catch for safety and do not throw error to upper levels
			try {
				persist(node);

				// Load node after update
				ClusterNode lockedNode = getClusterNodeByDn(node.getDn());

				// if lock is ours return it
				if (lockKey.equals(lockedNode.getLockKey())) {
				    log.debug("Successfully create new cluster node {}, lockKey {}", node.getId(), lockKey);
					return node;
				}
			} catch (EntryPersistenceException ex) {
				log.debug("Unexpected error happened during entry lock, lockKey " + lockKey, ex);
			}

            attempt++;
		} while (attempt <= ATTEMPT_LIMIT);

		// This should not happen
		throw new EntryPersistenceException("Failed to allocate ClusterNode!!! lockKey: " + lockKey);
	}

	public void release(ClusterNode clusterNode) {
		clusterNode.setLastUpdate(null);
		clusterNode.setCreationDate(null);
		clusterNode.setLockKey(null);

		update(clusterNode);
	}

	public void refresh(ClusterNode clusterNode) {
		clusterNode.setLastUpdate(new Date());

		update(clusterNode);
	}

	public ClusterNode reset(ClusterNode clusterNode) {
		Date currentTime = new Date();
		clusterNode.setCreationDate(currentTime);
		clusterNode.setLastUpdate(currentTime);

		update(clusterNode);

		return clusterNode;
	}

	public String getDnForClusterNode(Integer id) {
		return String.format("jansNum=%d,%s", id, staticConfiguration.getBaseDn().getNode());
	}

}
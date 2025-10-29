/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cluster;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.exception.ConfigurationException;
import io.jans.model.cluster.ClusterNode;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

/**
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
public abstract class ClusterNodeService {

    public static final int ATTEMPT_LIMIT = 10;

    public static final long DELAY_AFTER_EXPIRATION = 3 * 60 * 1000L; // 3 minutes
    public static final String JANS_TYPE_ATTR_NAME = "jansType";

    @Inject
    private Logger log;

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
        String clusterNodesBaseDn = getBaseClusterNodeDn();

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
        String clusterNodesBaseDn = getBaseClusterNodeDn();

        int count = 1;
        if (PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(entryManager.getPersistenceType(clusterNodesBaseDn))) {
            count = Integer.MAX_VALUE;
        }

        PagedResult<ClusterNode> pagedResult = entryManager.findPagedEntries(clusterNodesBaseDn, ClusterNode.class,
                Filter.createEqualityFilter(JANS_TYPE_ATTR_NAME, getClusterNodeType()), null, "jansNum", SortOrder.DESCENDING,
                0, count, count);
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
        String clusterNodesBaseDn = getBaseClusterNodeDn();
        if (StringUtils.isBlank(clusterNodesBaseDn)) {
            throw new ConfigurationException("ou=node is not configured in static configuration of AS (jansConfStatic).");
        }

        Date expirationDate = new Date(System.currentTimeMillis() - DELAY_AFTER_EXPIRATION);

        Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("jansType", getClusterNodeType()),
                Filter.createORFilter(Filter.createEqualityFilter("jansLastUpd", null), Filter.createLessOrEqualFilter(
                        "jansLastUpd", entryManager.encodeTime(clusterNodesBaseDn, expirationDate))));

        return entryManager.findEntries(clusterNodesBaseDn, ClusterNode.class, filter);
    }

    /**
     * returns a list of active ClusterNodes
     *
     * @return list of ClusterNodes
     */
    public List<ClusterNode> getClusterNodesLive() {
        String clusterNodesBaseDn = getBaseClusterNodeDn();
        if (StringUtils.isBlank(clusterNodesBaseDn)) {
            throw new ConfigurationException("ou=node is not configured in static configuration of AS (jansConfStatic).");
        }

        Date expirationDate = new Date(System.currentTimeMillis() - DELAY_AFTER_EXPIRATION);

        Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("jansType", getClusterNodeType()), Filter.createGreaterOrEqualFilter(
                        "jansLastUpd", entryManager.encodeTime(clusterNodesBaseDn, expirationDate)));

        return entryManager.findEntries(clusterNodesBaseDn, ClusterNode.class, filter);
    }

    @NotNull
    private Filter getTypeFilter() {
        return Filter.createEqualityFilter(JANS_TYPE_ATTR_NAME, getClusterNodeType());
    }

    protected void persist(ClusterNode clusterNode) {
        entryManager.persist(clusterNode);
    }

    public void update(ClusterNode clusterNode) {
        entryManager.merge(clusterNode);
    }

    // all logs must be INFO here, because allocate method is called during initialization before
    // LoggerService set loggingLevel from config.
    public ClusterNode allocate() {
        log.info("Allocating node, getLockKey() {}... ", getLockKey());

        // Try to use existing expired entry (node is expired if not used for 3 minutes)
        List<ClusterNode> expiredNodes = getClusterNodesExpired();
        log.info("Allocation - found {} expired nodes.", expiredNodes.size());

        for (ClusterNode expiredNode : expiredNodes) {
            // Do lock operation in try/catch for safety and do not throw error to upper levels
            try {
                Date currentTime = new Date();

                expiredNode.setCreationDate(currentTime);
                expiredNode.setLastUpdate(currentTime);
                expiredNode.setLockKey(getLockKey());

                update(expiredNode);

                // Load node after update
                ClusterNode lockedNode = getClusterNodeByDn(expiredNode.getDn());

                // If lock is ours reset entry and return it
                if (getLockKey().equals(lockedNode.getLockKey())) {
                    log.info("Re-using existing node {}, getLockKey() {}", lockedNode.getId(), getLockKey());
                    return lockedNode;
                }

                log.info("Failed to lock node {}, getLockKey() {}", lockedNode.getId(), getLockKey());
            } catch (EntryPersistenceException ex) {
                log.debug("Unexpected error happened during entry lock", ex);
            }
        }

        // There are no free entries. server need to add new one with next index
        int attempt = 1;
        do {
            log.info("Attempting to persist new node. Attempt {} out of {} ...", attempt, ATTEMPT_LIMIT);

            ClusterNode lastClusterNode = getClusterNodeLast();
            log.info("lastClusterNode - {}, getLockKey() {}", lastClusterNode != null ? lastClusterNode.getId() : -1, getLockKey());

            Integer lastClusterNodeIndex = lastClusterNode == null ? 0 : lastClusterNode.getId() + 1;

            Date currentTime = new Date();

            ClusterNode node = new ClusterNode();
            node.setId(lastClusterNodeIndex);
            node.setDn(getDnForClusterNode(lastClusterNodeIndex));
            node.setCreationDate(currentTime);
            node.setLastUpdate(currentTime);
            node.setType(getClusterNodeType());
            node.setLockKey(getLockKey());

            // Do persist operation in try/catch for safety and do not throw error to upper levels
            try {
                persist(node);

                // Load node after update
                ClusterNode lockedNode = getClusterNodeByDn(node.getDn());

                // if lock is ours return it
                if (getLockKey().equals(lockedNode.getLockKey())) {
                    log.info("Successfully created new cluster node {}", node);
                    return lockedNode;
                } else {
                    log.info("Locked key does not match. nodeLockKey {} of node {}", lockedNode.getLockKey(), lockedNode.getId());
                }
            } catch (EntryPersistenceException ex) {
                log.debug("Unexpected error happened during entry lock, getLockKey() " + getLockKey(), ex);
            }

            attempt++;
        } while (attempt <= ATTEMPT_LIMIT);

        return null;
    }

    public void refresh(ClusterNode node) {
        node.setLastUpdate(new Date());

        log.trace("Refreshing node: {}", node);
        update(node);
    }

    public ClusterNode reset(ClusterNode node) {
        Date currentTime = new Date();
        node.setCreationDate(currentTime);
        node.setLastUpdate(currentTime);

        log.trace("Resetting node: {}", node);
        update(node);

        return node;
    }

    public abstract String getLockKey();

    public abstract String getClusterNodeType();

    public abstract String getBaseClusterNodeDn();

    public String getDnForClusterNode(Integer id) {
        return String.format("jansNum=%d,%s", id, getBaseClusterNodeDn());
    }

}
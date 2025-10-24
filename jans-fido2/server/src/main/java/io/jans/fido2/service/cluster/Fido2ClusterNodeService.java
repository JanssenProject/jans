/*
 * Janssen Project software is available under the MIT License (2008). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.cluster;

import io.jans.as.model.config.StaticConfiguration;
import io.jans.model.cluster.ClusterNode;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Cluster node service for FIDO2 metrics aggregation
 * Implements distributed locking mechanism to ensure only one node performs aggregation
 * 
 * @author FIDO2 Team
 * @version 1.0, 10/24/2025
 */
@ApplicationScoped
public class Fido2ClusterNodeService {

    public static final int ATTEMPT_LIMIT = 10;
    public static final long DELAY_AFTER_EXPIRATION = 3 * 60 * 1000L; // 3 minutes
    public static final String CLUSTER_TYPE_FIDO2 = "fido2";
    public static final String JANS_TYPE_ATTR_NAME = "jansType";
    
    // Unique lock key for this server instance
    public static final String LOCK_KEY = UUID.randomUUID().toString();

    @Inject
    private Logger log;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private PersistenceEntryManager entryManager;

    /**
     * Get cluster node by DN
     */
    public ClusterNode getClusterNodeByDn(String dn) {
        try {
            return entryManager.find(ClusterNode.class, dn);
        } catch (Exception e) {
            log.debug("Failed to get cluster node by DN: {}", dn, e);
            return null;
        }
    }

    /**
     * Get cluster node by ID
     */
    public ClusterNode getClusterNodeById(Integer id) {
        try {
            return entryManager.find(ClusterNode.class, getDnForClusterNode(id));
        } catch (Exception e) {
            log.debug("Failed to get cluster node by ID: {}", id, e);
            return null;
        }
    }

    /**
     * Get all FIDO2 cluster nodes
     */
    public List<ClusterNode> getAllClusterNodes() {
        String clusterNodesBaseDn = staticConfiguration.getBaseDn().getNode();
        if (StringUtils.isBlank(clusterNodesBaseDn)) {
            log.warn("ou=node is not configured in static configuration");
            return List.of();
        }

        try {
            return entryManager.findEntries(clusterNodesBaseDn, ClusterNode.class, getTypeFilter());
        } catch (Exception e) {
            log.error("Failed to get all cluster nodes", e);
            return List.of();
        }
    }

    /**
     * Get last cluster node (highest ID)
     */
    public ClusterNode getClusterNodeLast() {
        String clusterNodesBaseDn = staticConfiguration.getBaseDn().getNode();
        if (StringUtils.isBlank(clusterNodesBaseDn)) {
            return null;
        }

        try {
            int count = 1;
            if (PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(entryManager.getPersistenceType(clusterNodesBaseDn))) {
                count = Integer.MAX_VALUE;
            }

            PagedResult<ClusterNode> pagedResult = entryManager.findPagedEntries(clusterNodesBaseDn, ClusterNode.class,
                    Filter.createEqualityFilter("jansType", CLUSTER_TYPE_FIDO2), null, "jansNum", SortOrder.DESCENDING,
                    0, count, count);
            
            if (pagedResult.getEntriesCount() >= 1) {
                return pagedResult.getEntries().get(0);
            }
        } catch (Exception e) {
            log.debug("Failed to get last cluster node", e);
        }

        return null;
    }

    /**
     * Get expired cluster nodes (not updated for 3+ minutes)
     */
    public List<ClusterNode> getClusterNodesExpired() {
        String clusterNodesBaseDn = staticConfiguration.getBaseDn().getNode();
        if (StringUtils.isBlank(clusterNodesBaseDn)) {
            return List.of();
        }

        try {
            Date expirationDate = new Date(System.currentTimeMillis() - DELAY_AFTER_EXPIRATION);

            Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansType", CLUSTER_TYPE_FIDO2),
                Filter.createORFilter(
                    Filter.createEqualityFilter("jansLastUpd", null),
                    Filter.createLessOrEqualFilter("jansLastUpd", entryManager.encodeTime(clusterNodesBaseDn, expirationDate))
                )
            );

            return entryManager.findEntries(clusterNodesBaseDn, ClusterNode.class, filter);
        } catch (Exception e) {
            log.error("Failed to get expired cluster nodes", e);
            return List.of();
        }
    }

    /**
     * Allocate a cluster node for this server instance
     * Uses distributed locking to ensure only one server gets the lock
     */
    public ClusterNode allocate() {
        log.info("Allocating FIDO2 cluster node, LOCK_KEY: {}...", LOCK_KEY);

        // Try to reuse expired nodes first
        List<ClusterNode> expiredNodes = getClusterNodesExpired();
        log.info("Found {} expired FIDO2 cluster nodes", expiredNodes.size());

        for (ClusterNode expiredNode : expiredNodes) {
            try {
                Date currentTime = new Date();
                expiredNode.setCreationDate(currentTime);
                expiredNode.setLastUpdate(currentTime);
                expiredNode.setLockKey(LOCK_KEY);

                update(expiredNode);

                // Verify we got the lock
                ClusterNode lockedNode = getClusterNodeByDn(expiredNode.getDn());
                if (lockedNode != null && LOCK_KEY.equals(lockedNode.getLockKey())) {
                    log.info("Re-using existing FIDO2 cluster node {}, LOCK_KEY: {}", lockedNode.getId(), LOCK_KEY);
                    return lockedNode;
                }

                log.debug("Failed to lock FIDO2 cluster node {}", expiredNode.getId());
            } catch (EntryPersistenceException ex) {
                log.debug("Failed to lock expired node", ex);
            }
        }

        // Create new node if no expired nodes available
        int attempt = 1;
        do {
            log.info("Attempting to create new FIDO2 cluster node. Attempt {} of {}...", attempt, ATTEMPT_LIMIT);

            ClusterNode lastNode = getClusterNodeLast();
            Integer nextId = lastNode == null ? 0 : lastNode.getId() + 1;

            Date currentTime = new Date();
            ClusterNode node = new ClusterNode();
            node.setId(nextId);
            node.setDn(getDnForClusterNode(nextId));
            node.setCreationDate(currentTime);
            node.setLastUpdate(currentTime);
            node.setType(CLUSTER_TYPE_FIDO2);
            node.setLockKey(LOCK_KEY);

            try {
                persist(node);

                // Verify we got the lock
                ClusterNode lockedNode = getClusterNodeByDn(node.getDn());
                if (lockedNode != null && LOCK_KEY.equals(lockedNode.getLockKey())) {
                    log.info("Successfully created FIDO2 cluster node {}", node.getId());
                    return lockedNode;
                }

                log.debug("Lock key mismatch for node {}", nextId);
            } catch (EntryPersistenceException ex) {
                log.debug("Failed to persist new node", ex);
            }

            attempt++;
        } while (attempt <= ATTEMPT_LIMIT);

        log.warn("Failed to allocate FIDO2 cluster node after {} attempts", ATTEMPT_LIMIT);
        return null;
    }

    /**
     * Refresh node timestamp to keep it alive
     */
    public void refresh(ClusterNode node) {
        if (node == null) {
            return;
        }

        try {
            node.setLastUpdate(new Date());
            log.trace("Refreshing FIDO2 cluster node: {}", node.getId());
            update(node);
        } catch (Exception e) {
            log.error("Failed to refresh FIDO2 cluster node: {}", node.getId(), e);
        }
    }

    /**
     * Reset node timestamps
     */
    public ClusterNode reset(ClusterNode node) {
        if (node == null) {
            return null;
        }

        try {
            Date currentTime = new Date();
            node.setCreationDate(currentTime);
            node.setLastUpdate(currentTime);

            log.trace("Resetting FIDO2 cluster node: {}", node.getId());
            update(node);

            return node;
        } catch (Exception e) {
            log.error("Failed to reset FIDO2 cluster node: {}", node.getId(), e);
            return node;
        }
    }

    /**
     * Check if this server instance holds the lock for the given node
     */
    public boolean hasLock(ClusterNode node) {
        if (node == null) {
            return false;
        }

        return LOCK_KEY.equals(node.getLockKey());
    }

    /**
     * Release the lock by marking node as expired
     */
    public void releaseLock(ClusterNode node) {
        if (node == null || !hasLock(node)) {
            return;
        }

        try {
            // Set last update to past to make it expired
            Date pastDate = new Date(System.currentTimeMillis() - (DELAY_AFTER_EXPIRATION + 60000));
            node.setLastUpdate(pastDate);
            update(node);
            log.info("Released lock for FIDO2 cluster node {}", node.getId());
        } catch (Exception e) {
            log.error("Failed to release lock for FIDO2 cluster node: {}", node.getId(), e);
        }
    }

    // Private helper methods

    private Filter getTypeFilter() {
        return Filter.createEqualityFilter(JANS_TYPE_ATTR_NAME, CLUSTER_TYPE_FIDO2);
    }

    private void persist(ClusterNode clusterNode) {
        entryManager.persist(clusterNode);
    }

    private void update(ClusterNode clusterNode) {
        entryManager.merge(clusterNode);
    }

    private String getDnForClusterNode(Integer id) {
        return String.format("jansNum=%d,%s", id, staticConfiguration.getBaseDn().getNode());
    }
}


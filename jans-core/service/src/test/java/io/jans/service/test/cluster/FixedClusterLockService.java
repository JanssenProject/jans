/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test.cluster;

import java.time.Clock;

import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cluster.ClusterLockService;

/**
 * {@link ClusterLockService} with fixed settings and an explicit entry manager, for tests without CDI.
 */
public class FixedClusterLockService extends ClusterLockService {

    public static final String DEFAULT_BASE_DN = "ou=node,o=jans";

    private final PersistenceEntryManager entryManager;
    private final String baseDn;
    private final String lockKey;
    private final String nodeIdentifier;
    private final Integer clusterNodeId;

    public FixedClusterLockService(PersistenceEntryManager entryManager, String lockKey, Clock clock) {
        this(entryManager, DEFAULT_BASE_DN, lockKey, "node-" + lockKey, null, clock);
    }

    public FixedClusterLockService(PersistenceEntryManager entryManager, String baseDn, String lockKey, String nodeIdentifier,
            Integer clusterNodeId, Clock clock) {
        this.entryManager = entryManager;
        this.baseDn = baseDn;
        this.lockKey = lockKey;
        this.nodeIdentifier = nodeIdentifier;
        this.clusterNodeId = clusterNodeId;
        if (clock != null) {
            setClock(clock);
        }
    }

    @Override
    public PersistenceEntryManager getEntryManager() {
        return entryManager;
    }

    @Override
    protected Integer getClusterNodeId() {
        return clusterNodeId;
    }

    @Override
    public String getBaseDn() {
        return baseDn;
    }

    @Override
    public String getLockKey() {
        return lockKey;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

}

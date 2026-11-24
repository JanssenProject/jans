/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.model.config.StaticConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.as.server.service.MetricService;

/**
 * Cluster lock service of jans-auth. It stores the claims of the metric aggregation under the cluster node
 * branch and reports the cluster node id of this instance when one is allocated.
 */
@ApplicationScoped
public class ClusterLockService extends io.jans.service.cluster.ClusterLockService {

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private ClusterNodeManager clusterNodeManager;

    @Inject
    private MetricService metricService;

    @Override
    public String getBaseDn() {
        return staticConfiguration.getBaseDn().getNode();
    }

    @Override
    public String getLockKey() {
        return ClusterNodeService.LOCK_KEY;
    }

    @Override
    public String getNodeIdentifier() {
        return metricService.getNodeIdentifier();
    }

    /**
     * @return the cluster node id, or {@code null} when this instance has no allocated cluster node
     */
    @Override
    protected Integer getClusterNodeId() {
        try {
            return clusterNodeManager.getClusterNodeId();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

}

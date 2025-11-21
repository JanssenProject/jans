/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.cluster;

import io.jans.as.model.config.StaticConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Cluster node service for FIDO2 metrics aggregation
 * Extends the centralized ClusterNodeService from jans-core-service
 * 
 * @author FIDO2 Team
 * @version 2.0, 10/24/2025
 */
@ApplicationScoped
public class Fido2ClusterNodeService extends io.jans.service.cluster.ClusterNodeService {

    public static final String CLUSTER_TYPE_FIDO2 = "fido2";

    public static final String LOCK_KEY = UUID.randomUUID().toString();

    @Inject
    private StaticConfiguration staticConfiguration;

    @Override
    public String getClusterNodeType() {
        return CLUSTER_TYPE_FIDO2;
    }

    @Override
    public String getLockKey() {
        return LOCK_KEY;
    }

    @Override
    public String getBaseClusterNodeDn() {
        return staticConfiguration.getBaseDn().getNode();
    }
}


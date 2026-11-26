/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.cluster;

import org.apache.commons.lang3.StringUtils;

import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.fido2.service.shared.MetricService;
import io.jans.service.cluster.ClusterLockService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Cluster claims (entries of {@code jansClusterLock}) of the FIDO2 server. Used by
 * {@link io.jans.fido2.service.metric.Fido2MetricAggregationManager} so that one FIDO2 node builds the
 * metric aggregates of each run slot.
 */
@ApplicationScoped
public class Fido2ClusterLockService extends ClusterLockService {

    /** Same value as {@code baseDn.node} of the other Janssen servers. */
    public static final String DEFAULT_NODE_DN = "ou=node,o=jans";

    private static final String CONFIGURATION_RDN_PREFIX = "ou=configuration,";

    private static final String NODE_RDN_PREFIX = "ou=node,";

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private MetricService metricService;

    @Override
    protected io.jans.orm.PersistenceEntryManager getEntryManager() {
        return metricService.getEntryManager();
    }

    /**
     * The FIDO2 static configuration normally has no {@code node} entry (setting it would switch
     * {@code Fido2MetricsAggregationScheduler} into cluster mode), so the node branch is derived from
     * {@code baseDn.configuration}, which sits under the same root.
     */
    @Override
    public String getBaseDn() {
        BaseDnConfiguration baseDn = staticConfiguration.getBaseDn();
        if (baseDn == null) {
            return DEFAULT_NODE_DN;
        }

        if (StringUtils.isNotBlank(baseDn.getNode())) {
            return baseDn.getNode();
        }

        String configurationDn = baseDn.getConfiguration();
        if (StringUtils.startsWith(configurationDn, CONFIGURATION_RDN_PREFIX)) {
            return NODE_RDN_PREFIX + configurationDn.substring(CONFIGURATION_RDN_PREFIX.length());
        }

        return DEFAULT_NODE_DN;
    }

    @Override
    public String getLockKey() {
        return Fido2ClusterNodeService.LOCK_KEY;
    }

    @Override
    public String getNodeIdentifier() {
        return metricService.getNodeIdentifier();
    }
}

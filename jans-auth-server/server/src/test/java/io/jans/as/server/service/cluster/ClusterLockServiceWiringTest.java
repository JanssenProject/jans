/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.as.server.service.cluster;

import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.server.service.MetricService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks the hooks of the jans-auth cluster lock service without a CDI container.
 */
public class ClusterLockServiceWiringTest {

    private static final String NODE_BASE_DN = "ou=node,o=jans";

    @Test
    public void usesNodeBranchAndClusterLockKey() throws Exception {
        ClusterLockService service = newLockService(7);

        assertEquals(NODE_BASE_DN, service.getBaseDn());
        assertEquals(ClusterNodeService.LOCK_KEY, service.getLockKey());
        assertEquals("00:00:5e:00:53:af", service.getNodeIdentifier());
        assertEquals(Integer.valueOf(7), service.getClusterNodeId());
    }

    @Test
    public void returnsNullClusterNodeIdWhenNotAllocated() throws Exception {
        assertNull(newLockService(null).getClusterNodeId());
    }

    private static ClusterLockService newLockService(Integer clusterNodeId) throws Exception {
        StaticConfiguration staticConfiguration = new StaticConfiguration();
        BaseDnConfiguration baseDn = new BaseDnConfiguration();
        baseDn.setNode(NODE_BASE_DN);
        staticConfiguration.setBaseDn(baseDn);

        ClusterNodeManager nodeManager = new ClusterNodeManager() {
            @Override
            public Integer getClusterNodeId() {
                if (clusterNodeId == null) {
                    throw new IllegalStateException("Cluster node is not allocated.");
                }
                return clusterNodeId;
            }
        };

        MetricService metricService = new MetricService() {
            @Override
            public String getNodeIdentifier() {
                return "00:00:5e:00:53:af";
            }
        };

        ClusterLockService service = new ClusterLockService();
        set(service, "staticConfiguration", staticConfiguration);
        set(service, "clusterNodeManager", nodeManager);
        set(service, "metricService", metricService);
        return service;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

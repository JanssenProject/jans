/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.cluster;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.model.cluster.ClusterNode;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.test.cluster.FixedClusterNodeService;
import io.jans.service.test.orm.SqlTestProfile;

/**
 * SQL allocation for two types in the dedicated jans_node_allocation_test database with a seeded foreign-type node.
 * Only confirmed inserts owned by this test are removed, including after assertion failures.
 */
public class ClusterNodeServiceDbTest {

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private final String baseDn = "ou=t10-" + suffix + ",ou=node,o=jans";
    private final List<String> insertedDns = new ArrayList<>();
    private ClusterNode foreignNode;
    private final String typeA = "t10a-" + suffix;
    private final String typeB = "t10b-" + suffix;

    private PersistenceEntryManager entryManager;

    @BeforeClass
    public void createEntryManager() throws Exception {
        entryManager = SqlTestProfile.createEntryManager("jans_node_allocation_test");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        if (entryManager == null) {
            return;
        }
        RuntimeException errors = new IllegalStateException("Failed to clean up SQL node test");
        try {
            for (String dn : insertedDns) {
                try {
                    entryManager.remove(dn, ClusterNode.class);
                } catch (RuntimeException ex) {
                    errors.addSuppressed(ex);
                }
            }
        } finally {
            try {
                entryManager.destroy();
            } catch (RuntimeException ex) {
                errors.addSuppressed(ex);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    @Test
    public void twoTypesOnOneBaseDnOnDatabase() {
        foreignNode = new ClusterNode();
        foreignNode.setId(ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000));
        foreignNode.setDn(dn(foreignNode.getId()));
        foreignNode.setType("foreign-" + suffix);
        foreignNode.setLockKey("foreign-" + suffix);
        foreignNode.setCreationDate(new Date());
        foreignNode.setLastUpdate(foreignNode.getCreationDate());
        entryManager.persist(foreignNode);
        insertedDns.add(foreignNode.getDn());
        foreignNode = entryManager.find(ClusterNode.class, foreignNode.getDn());
        Map<String, String> otherNodes = describeNodes(Collections.singletonList(foreignNode));

        FixedClusterNodeService serviceA = trackingService(typeA, "t10-key-a-" + suffix);
        FixedClusterNodeService serviceB = trackingService(typeB, "t10-key-b-" + suffix);
        int maxIdBefore = foreignNode.getId();

        ClusterNode nodeA = serviceA.allocate();
        ClusterNode nodeB = serviceB.allocate();

        assertNotNull(nodeA, "Type A gets a node");
        assertNotNull(nodeB, "Type B gets a node");
        assertTrue(nodeA.getId() > maxIdBefore, "Id of type A " + nodeA.getId() + " is above " + maxIdBefore);
        assertTrue(nodeB.getId() > nodeA.getId(), "Id of type B " + nodeB.getId() + " is above " + nodeA.getId());
        assertEquals(lastOf(serviceA.getPersistedDns()), nodeA.getDn());
        assertEquals(lastOf(serviceB.getPersistedDns()), nodeB.getDn());
        assertOnlyNode(typeA, nodeA, serviceA.getLockKey());
        assertOnlyNode(typeB, nodeB, serviceB.getLockKey());

        assertEquals(describeNodes(Collections.singletonList(entryManager.find(ClusterNode.class, foreignNode.getDn()))),
                otherNodes, "The seeded foreign-type node is unchanged");
    }

    private void assertOnlyNode(String type, ClusterNode expected, String lockKey) {
        List<ClusterNode> nodes = findNodes(type);
        assertEquals(nodes.size(), 1, "Rows of type " + type);
        assertEquals(nodes.get(0).getDn(), expected.getDn());
        assertEquals(nodes.get(0).getId(), expected.getId());
        assertEquals(nodes.get(0).getLockKey(), lockKey);
    }

    private static String lastOf(List<String> values) {
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    private FixedClusterNodeService trackingService(String type, String key) {
        return new FixedClusterNodeService(entryManager, baseDn, type, key) {
            @Override
            protected void persist(ClusterNode node) {
                super.persist(node);
                insertedDns.add(node.getDn());
            }
        };
    }

    private List<ClusterNode> findNodes(String type) {
        return entryManager.findEntries(baseDn, ClusterNode.class, Filter.createEqualityFilter(ClusterNodeService.JANS_TYPE_ATTR_NAME, type));
    }

    /**
     * @return DN to id, type, lock key and creation date of the seeded foreign node;
     *         {@code jansLastUpd} is left out because live nodes refresh it
     */
    private Map<String, String> describeNodes(List<ClusterNode> nodes) {
        Map<String, String> result = new TreeMap<>();
        for (ClusterNode node : nodes) {
            if (typeA.equals(node.getType()) || typeB.equals(node.getType())) {
                continue;
            }
            result.put(node.getDn(), node.getId() + "|" + node.getType() + "|" + node.getLockKey() + "|"
                    + (node.getCreationDate() == null ? null : node.getCreationDate().getTime()));
        }

        return result;
    }

    private String dn(int id) {
        return String.format("jansNum=%d,%s", id, baseDn);
    }

}

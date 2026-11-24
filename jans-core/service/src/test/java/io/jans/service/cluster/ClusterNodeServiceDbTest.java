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
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.model.cluster.ClusterNode;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.test.cluster.FixedClusterNodeService;
import io.jans.service.test.orm.SqlTestProfile;

/**
 * {@link ClusterNodeService#allocate()} for two node types on one base DN of a real SQL database.
 *
 * <p>Skipped unless the system property {@value SqlTestProfile#PROFILE_PROPERTY} names a profile
 * directory of {@code jans-orm/integration-test}. It also needs an existing node with id 0 of another
 * type under {@code ou=node,o=jans} (a running jans-auth creates it).</p>
 *
 * <p>Both node types are random, so {@code allocate()} never finds an expired node of its own type and
 * never updates an existing row: the only writes are inserts of new rows. Every row of the two types is
 * removed in {@link #cleanUp()}, also after a failure. Rows of other types are compared before and after
 * the test.</p>
 *
 * <p>Both types get a node with an id above the highest id of all types that existed before.</p>
 */
public class ClusterNodeServiceDbTest {

    private static final String BASE_DN = "ou=node,o=jans";
    private static final int REMOVE_LIMIT = 100;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private final String typeA = "t10a-" + suffix;
    private final String typeB = "t10b-" + suffix;

    private PersistenceEntryManager entryManager;

    @BeforeClass
    public void createEntryManager() throws Exception {
        entryManager = SqlTestProfile.createEntryManager();
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {
        if (entryManager == null) {
            return;
        }

        try {
            for (String type : new String[] { typeA, typeB }) {
                Filter byType = Filter.createEqualityFilter(ClusterNodeService.JANS_TYPE_ATTR_NAME, type);
                entryManager.remove(BASE_DN, ClusterNode.class, byType, REMOVE_LIMIT);
                for (ClusterNode left : entryManager.findEntries(BASE_DN, ClusterNode.class, byType)) {
                    entryManager.remove(left.getDn(), ClusterNode.class);
                }
            }
        } finally {
            entryManager.destroy();
        }
    }

    @Test
    public void twoTypesOnOneBaseDnOnDatabase() {
        Map<String, String> otherNodes = describeNodes(findAllNodes());
        if (!otherNodes.containsKey(dn(0))) {
            throw new SkipException("Needs an existing node with id 0 under " + BASE_DN);
        }
        assertTrue(findNodes(typeA).isEmpty(), "Random type already exists: " + typeA);
        assertTrue(findNodes(typeB).isEmpty(), "Random type already exists: " + typeB);

        FixedClusterNodeService serviceA = new FixedClusterNodeService(entryManager, BASE_DN, typeA, "t10-key-a-" + suffix);
        FixedClusterNodeService serviceB = new FixedClusterNodeService(entryManager, BASE_DN, typeB, "t10-key-b-" + suffix);

        int maxIdBefore = 0;
        for (ClusterNode node : findAllNodes()) {
            maxIdBefore = Math.max(maxIdBefore, node.getId());
        }

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

        assertEquals(describeNodes(findAllNodes()), otherNodes, "Nodes of other types are unchanged");
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

    private List<ClusterNode> findAllNodes() {
        return entryManager.findEntries(BASE_DN, ClusterNode.class, Filter.createPresenceFilter("jansNum"));
    }

    private List<ClusterNode> findNodes(String type) {
        return entryManager.findEntries(BASE_DN, ClusterNode.class, Filter.createEqualityFilter(ClusterNodeService.JANS_TYPE_ATTR_NAME, type));
    }

    /**
     * @return DN to id, type, lock key and creation date of each node, without the test's own types;
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

    private static String dn(int id) {
        return String.format("jansNum=%d,%s", id, BASE_DN);
    }

}

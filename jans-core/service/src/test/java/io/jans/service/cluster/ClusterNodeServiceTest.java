/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.cluster;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

import io.jans.model.cluster.ClusterNode;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.test.cluster.FixedClusterNodeService;
import io.jans.service.test.orm.InMemoryEntryManager;

/**
 * Id allocation of {@link ClusterNodeService#allocate()} with the in-memory entry manager.
 *
 * <p>Two node types share one base DN, and the DN {@code jansNum=<id>,<baseDn>} does not contain the
 * type. A new id is therefore above the nodes of all types, while only expired nodes of the own type are
 * re-used.</p>
 */
public class ClusterNodeServiceTest {

    private static final String BASE_DN = "ou=node,o=jans";
    private static final String TYPE_A = "type-a";
    private static final String TYPE_B = "type-b";
    private static final long EXPIRED_MILLIS = ClusterNodeService.DELAY_AFTER_EXPIRATION + 60_000L;

    @Test
    public void sameTypeGetsNextIdAboveItsOwnLastNode() {
        InMemoryEntryManager fake = new InMemoryEntryManager();
        PersistenceEntryManager entryManager = fake.getEntryManager();

        ClusterNode first = new FixedClusterNodeService(entryManager, BASE_DN, TYPE_A, "key-a1").allocate();
        ClusterNode second = new FixedClusterNodeService(entryManager, BASE_DN, TYPE_A, "key-a2").allocate();

        assertNode(first, 0, TYPE_A, "key-a1");
        assertNode(second, 1, TYPE_A, "key-a2");
        assertEquals(fake.size(), 2);
    }

    @Test
    public void secondTypeGetsIdAboveNodesOfAllTypes() {
        InMemoryEntryManager fake = new InMemoryEntryManager();
        PersistenceEntryManager entryManager = fake.getEntryManager();
        FixedClusterNodeService serviceA = new FixedClusterNodeService(entryManager, BASE_DN, TYPE_A, "key-a");
        FixedClusterNodeService serviceB = new FixedClusterNodeService(entryManager, BASE_DN, TYPE_B, "key-b");

        ClusterNode nodeA = serviceA.allocate();
        ClusterNode nodeB = serviceB.allocate();

        assertNode(nodeA, 0, TYPE_A, "key-a");
        assertNode(nodeB, 1, TYPE_B, "key-b");
        assertEquals(serviceB.getPersistedDns(), Collections.singletonList(dn(1)));
        assertEquals(fake.size(), 2);
        assertNode(fake.get(dn(0), ClusterNode.class), 0, TYPE_A, "key-a");
        assertNode(fake.get(dn(1), ClusterNode.class), 1, TYPE_B, "key-b");
    }

    @Test
    public void newTypeGetsIdAboveNodesOfAnotherTypeOnLdap() {
        InMemoryEntryManager fake = new InMemoryEntryManager(PersistenceEntryManager.PERSITENCE_TYPES.ldap.name());
        List<Object> pageSizes = recordPageSizes(fake);
        PersistenceEntryManager entryManager = fake.getEntryManager();
        long now = System.currentTimeMillis();
        fake.put(node(0, TYPE_A, "key-a0", new Date(now)));
        fake.put(node(1, TYPE_A, "key-a1", new Date(now - EXPIRED_MILLIS)));
        FixedClusterNodeService serviceB = new FixedClusterNodeService(entryManager, BASE_DN, TYPE_B, "key-b");

        ClusterNode nodeB = serviceB.allocate();

        assertNode(nodeB, 2, TYPE_B, "key-b");
        assertEquals(serviceB.getPersistedDns(), Collections.singletonList(dn(2)));
        assertEquals(pageSizes, Collections.singletonList((Object) Integer.MAX_VALUE),
                "On LDAP the last node is searched without a page limit");
        assertEquals(fake.size(), 3);
        assertNode(fake.get(dn(0), ClusterNode.class), 0, TYPE_A, "key-a0");
        assertNode(fake.get(dn(1), ClusterNode.class), 1, TYPE_A, "key-a1");

        ClusterNode reused = new FixedClusterNodeService(entryManager, BASE_DN, TYPE_A, "key-a2").allocate();

        assertNode(reused, 1, TYPE_A, "key-a2");
        assertEquals(fake.size(), 3);
    }

    @Test
    public void insertThatLosesARaceRetriesAboveTheWinner() {
        InMemoryEntryManager fake = new InMemoryEntryManager();
        PersistenceEntryManager entryManager = fake.getEntryManager();
        long now = System.currentTimeMillis();
        fake.put(node(0, TYPE_A, "key-a0", new Date(now)));
        AtomicBoolean raced = new AtomicBoolean();
        fake.setFailureInjector((method, args) -> {
            if ("persistNew".equals(method) && raced.compareAndSet(false, true)) {
                fake.put(node(1, TYPE_A, "key-a1", new Date(now)));
            }
            return null;
        });
        FixedClusterNodeService serviceB = new FixedClusterNodeService(entryManager, BASE_DN, TYPE_B, "key-b");

        ClusterNode nodeB = serviceB.allocate();

        assertNode(nodeB, 2, TYPE_B, "key-b");
        assertEquals(serviceB.getPersistedDns(), Arrays.asList(dn(1), dn(2)));
        assertNode(fake.get(dn(1), ClusterNode.class), 1, TYPE_A, "key-a1");
        assertEquals(fake.size(), 3);
    }

    @Test
    public void lastNodeIsSearchedWithPageSizeOneOnSql() {
        InMemoryEntryManager fake = new InMemoryEntryManager();
        List<Object> pageSizes = recordPageSizes(fake);

        ClusterNode node = new FixedClusterNodeService(fake.getEntryManager(), BASE_DN, TYPE_A, "key-a").allocate();

        assertNode(node, 0, TYPE_A, "key-a");
        assertEquals(pageSizes, Collections.singletonList((Object) 1));
    }

    private static List<Object> recordPageSizes(InMemoryEntryManager fake) {
        List<Object> pageSizes = new CopyOnWriteArrayList<>();
        fake.setFailureInjector((method, args) -> {
            if ("findPagedEntries".equals(method)) {
                pageSizes.add(args[7]);
            }
            return null;
        });

        return pageSizes;
    }

    private static void assertNode(ClusterNode node, int id, String type, String lockKey) {
        assertNotNull(node);
        assertEquals(node.getId(), Integer.valueOf(id));
        assertEquals(node.getDn(), dn(id));
        assertEquals(node.getType(), type);
        assertEquals(node.getLockKey(), lockKey);
    }

    private static ClusterNode node(int id, String type, String lockKey, Date lastUpdate) {
        ClusterNode node = new ClusterNode();
        node.setId(id);
        node.setDn(dn(id));
        node.setType(type);
        node.setLockKey(lockKey);
        node.setCreationDate(lastUpdate);
        node.setLastUpdate(lastUpdate);

        return node;
    }

    private static String dn(int id) {
        return String.format("jansNum=%d,%s", id, BASE_DN);
    }

}

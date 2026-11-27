/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.cluster;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.jans.model.cluster.ClusterLock;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.service.test.MutableClock;
import io.jans.service.test.cluster.FixedClusterLockService;
import io.jans.service.test.orm.InMemoryEntryManager;

public class ClusterLockServiceTest {

    private static final String TASK = "metric-aggregation_jans_auth";
    private static final Duration LEASE = Duration.ofMinutes(10);
    private static final Instant START = Instant.parse("2026-11-24T10:00:00Z");

    private InMemoryEntryManager store;
    private MutableClock clock;
    private FixedClusterLockService nodeA;
    private FixedClusterLockService nodeB;

    @BeforeMethod
    public void setUp() {
        store = new InMemoryEntryManager();
        clock = new MutableClock(START);
        nodeA = new FixedClusterLockService(store.getEntryManager(), FixedClusterLockService.DEFAULT_BASE_DN, "key-a", "host-a", 3, clock);
        nodeB = new FixedClusterLockService(store.getEntryManager(), "key-b", clock);
    }

    @Test
    public void claimStoresRunningEntry() {
        Optional<ClusterLock> claim = nodeA.tryClaim(TASK, "5900000", LEASE);

        assertTrue(claim.isPresent());
        String dn = "jansId=" + TASK + "_5900000,ou=node,o=jans";
        assertEquals(claim.get().getDn(), dn);

        ClusterLock stored = store.get(dn, ClusterLock.class);
        assertNotNull(stored);
        assertEquals(stored.getId(), TASK + "_5900000");
        assertEquals(stored.getTask(), TASK);
        assertEquals(stored.getLockKey(), claim.get().getLockKey());
        assertEquals(stored.getNodeIdentifier(), "host-a");
        assertEquals(stored.getClusterNodeId(), Integer.valueOf(3));
        assertEquals(stored.getStatus(), ClusterLock.STATUS_RUNNING);
        assertEquals(stored.getDeletable(), Boolean.TRUE);
        assertEquals(stored.getCreationDate().toInstant(), START);
        assertEquals(stored.getLastUpdate().toInstant(), START);
        assertEquals(stored.getExpirationDate().toInstant(), START.plus(LEASE));
        assertEquals(stored.getTtl(), Integer.valueOf((int) LEASE.getSeconds()));
        assertTrue(nodeA.isActive(stored, START));
    }

    @Test
    public void storeKeepsCopiesNotCallerObjects() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        String token = claim.getLockKey();
        claim.setLockKey("changed");

        assertEquals(store.get(claim.getDn(), ClusterLock.class).getLockKey(), token);
    }

    @Test
    public void secondClaimOfSameSlotIsRefused() {
        ClusterLock first = nodeA.tryClaim(TASK, "1", LEASE).get();

        assertFalse(nodeB.tryClaim(TASK, "1", LEASE).isPresent());
        assertFalse(nodeA.tryClaim(TASK, "1", LEASE).isPresent());
        assertEquals(store.get(nodeA.buildClaimDn(TASK, "1"), ClusterLock.class).getLockKey(), first.getLockKey());

        assertTrue(nodeB.tryClaim(TASK, "2", LEASE).isPresent());
        assertTrue(nodeB.tryClaim("other-task", "1", LEASE).isPresent());
    }

    @Test
    public void fakeThrowsSqlDuplicateChain() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();

        EntryPersistenceException ex = expectThrows(EntryPersistenceException.class, () -> store.getEntryManager().persist(claim));
        assertTrue(ex.getCause() instanceof DuplicateEntryException);
        assertEquals(((DuplicateEntryException) ex.getCause()).getDn(), claim.getDn());
        assertTrue(ClusterLockService.isDuplicate(ex));
    }

    @Test
    public void heartbeatByOwnerMovesLastUpdateAndExpiration() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        Instant later = clock.advance(Duration.ofMinutes(7));

        assertTrue(nodeA.heartbeat(claim));

        ClusterLock stored = store.get(claim.getDn(), ClusterLock.class);
        assertEquals(stored.getLastUpdate().toInstant(), later);
        assertEquals(stored.getExpirationDate().toInstant(), later.plus(LEASE));
        assertEquals(stored.getCreationDate().toInstant(), START);
        assertEquals(claim.getExpirationDate().toInstant(), later.plus(LEASE));
        assertTrue(nodeA.isActive(stored, START.plus(LEASE).plusSeconds(1)));
    }

    @Test
    public void heartbeatByStrangerFailsAndChangesNothing() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        Map<String, Object> before = InMemoryEntryManager.describe(store.get(claim.getDn(), ClusterLock.class));
        clock.advance(Duration.ofMinutes(1));

        assertFalse(nodeB.heartbeat(claim));
        assertFalse(nodeB.heartbeat(nodeB.find(TASK, "1").get()));

        assertEquals(InMemoryEntryManager.describe(store.get(claim.getDn(), ClusterLock.class)), before);
        assertEquals(store.getCallCount("merge"), 0);
    }

    @Test
    public void heartbeatOfMissingClaimFails() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        store.clear();

        assertFalse(nodeA.heartbeat(claim));
    }

    @Test
    public void heartbeatFailsWhenUpdateFails() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        store.setFailureInjector((method, args) -> "merge".equals(method) ? new EntryPersistenceException("down") : null);

        assertFalse(nodeA.heartbeat(claim));
    }

    @Test
    public void releaseByOwnerKeepsRowForRetention() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        Instant later = clock.advance(Duration.ofMinutes(3));

        assertTrue(nodeA.release(claim, ClusterLock.STATUS_DONE, "{\"rows\":5}"));

        ClusterLock stored = store.get(claim.getDn(), ClusterLock.class);
        assertEquals(stored.getStatus(), ClusterLock.STATUS_DONE);
        assertEquals(stored.getData(), "{\"rows\":5}");
        assertEquals(stored.getLastUpdate().toInstant(), later);
        assertEquals(stored.getExpirationDate().toInstant(), later.plus(ClusterLockService.RELEASED_RETENTION));
        assertEquals(stored.getLockKey(), claim.getLockKey());
        assertFalse(nodeA.isActive(stored, later));
        assertFalse(nodeA.heartbeat(claim));
    }

    @Test
    public void releaseByStrangerFailsAndChangesNothing() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        Map<String, Object> before = InMemoryEntryManager.describe(store.get(claim.getDn(), ClusterLock.class));

        assertFalse(nodeB.release(claim, ClusterLock.STATUS_SKIPPED, null));

        assertEquals(InMemoryEntryManager.describe(store.get(claim.getDn(), ClusterLock.class)), before);
    }

    @Test
    public void releaseRejectsInvalidStatus() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();

        expectThrows(IllegalArgumentException.class, () -> nodeA.release(claim, ClusterLock.STATUS_RUNNING, null));
        expectThrows(IllegalArgumentException.class, () -> nodeA.release(claim, "", null));
        expectThrows(IllegalArgumentException.class, () -> nodeA.release(claim, "status-longer-than-16", null));
    }

    @Test
    public void isActiveFollowsLease() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();

        assertTrue(nodeB.isActive(claim, START.plus(LEASE).minusMillis(1)));
        assertFalse(nodeB.isActive(claim, START.plus(LEASE)));
        assertFalse(nodeB.isActive(null, START));
    }

    @Test
    public void activeClaimSearchExcludesSelfReleasedExpiredAndOtherTasks() {
        nodeA.tryClaim(TASK, "current", LEASE).get();
        nodeB.tryClaim("other-task", "1", LEASE).get();
        ClusterLock released = nodeB.tryClaim(TASK, "skipped", LEASE).get();
        assertTrue(nodeB.release(released, ClusterLock.STATUS_SKIPPED, null));
        nodeB.tryClaim(TASK, "expired", Duration.ofSeconds(1)).get();
        clock.advance(Duration.ofSeconds(2));
        assertFalse(nodeA.findActive(TASK, "current", clock.instant()).isPresent());

        ClusterLock running = nodeB.tryClaim(TASK, "older", LEASE).get();
        assertEquals(nodeA.findActive(TASK, "current", clock.instant()).get().getId(), running.getId());
        assertTrue(nodeB.release(running, ClusterLock.STATUS_DONE, null));
        assertFalse(nodeA.findActive(TASK, "current", clock.instant()).isPresent());
    }

    @Test
    public void findReturnsClaimOrEmpty() {
        assertFalse(nodeA.find(TASK, "1").isPresent());

        ClusterLock first = nodeA.tryClaim(TASK, "1", LEASE).get();

        Optional<ClusterLock> found = nodeB.find(TASK, "1");
        assertTrue(found.isPresent());
        assertEquals(found.get().getLockKey(), first.getLockKey());
    }

    @Test
    public void findPropagatesReadError() {
        nodeA.tryClaim(TASK, "1", LEASE);
        store.setFailureInjector((method, args) -> "find".equals(method)
                ? new EntryPersistenceException("Failed to find entry", new SearchException("query failed", new RuntimeException("io")))
                : null);

        expectThrows(EntryPersistenceException.class, () -> nodeA.find(TASK, "1"));
    }

    @Test
    public void purgeRemovesOnlyExpiredClaimsOfTask() {
        ClusterLock released = nodeA.tryClaim(TASK, "1", LEASE).get();
        nodeA.release(released, ClusterLock.STATUS_DONE, null);
        nodeA.tryClaim(TASK, "2", LEASE);
        nodeA.tryClaim("other-task", "1", LEASE);
        assertTrue(nodeA.saveState(nodeA.find(TASK, "2").get(), current -> "{}"));

        clock.advance(Duration.ofMinutes(30));
        nodeA.tryClaim(TASK, "3", LEASE);

        assertEquals(nodeA.purgeExpired(TASK, 100), 1);
        assertFalse(nodeA.find(TASK, "2").isPresent());
        assertTrue(nodeA.find(TASK, "1").isPresent());
        assertTrue(nodeA.find(TASK, "3").isPresent());
        assertTrue(nodeA.find("other-task", "1").isPresent());

        clock.advance(ClusterLockService.RELEASED_RETENTION);

        assertEquals(nodeA.purgeExpired(TASK, 100), 2);
        assertFalse(nodeA.find(TASK, "1").isPresent());
        assertFalse(nodeA.find(TASK, "3").isPresent());
        assertTrue(nodeA.find("other-task", "1").isPresent());
        assertEquals(nodeA.loadState(TASK), Optional.of("{}"));
    }

    @Test
    public void purgeHonorsLimit() {
        for (int i = 0; i < 5; i++) {
            nodeA.tryClaim(TASK, String.valueOf(i), LEASE);
        }
        clock.advance(LEASE);

        assertEquals(nodeA.purgeExpired(TASK, 2), 2);
        assertEquals(nodeA.purgeExpired(TASK, 100), 3);
        assertEquals(store.size(), 0);
    }

    @Test
    public void stateRowIsCreatedUpdatedAndNeverExpires() {
        assertEquals(nodeA.loadState(TASK), Optional.empty());

        ClusterLock first = nodeA.tryClaim(TASK, "1", LEASE).get();
        assertTrue(nodeA.saveState(first, current -> "{\"HOURLY\":\"2026-11-24T09:00:00Z\"}"));
        ClusterLock state = store.get("jansId=" + TASK + "_state,ou=node,o=jans", ClusterLock.class);
        assertNotNull(state);
        assertEquals(state.getStatus(), ClusterLock.STATUS_STATE);
        assertEquals(state.getDeletable(), Boolean.FALSE);
        assertNull(state.getExpirationDate());
        assertNull(state.getTtl());

        clock.advance(Duration.ofHours(1));
        ClusterLock second = nodeB.tryClaim(TASK, "2", LEASE).get();
        assertTrue(nodeB.saveState(second, current -> "{\"HOURLY\":\"2026-11-24T10:00:00Z\"}"));

        assertEquals(nodeA.loadState(TASK), Optional.of("{\"HOURLY\":\"2026-11-24T10:00:00Z\"}"));
        state = store.get(nodeA.buildStateDn(TASK), ClusterLock.class);
        assertEquals(state.getLockKey(), second.getLockKey());
        assertEquals(state.getCreationDate().toInstant(), START);
        assertNull(state.getExpirationDate());
    }

    @Test
    public void failedStateRereadLeavesStoredValueUnchanged() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        assertTrue(nodeA.saveState(claim, current -> "{\"by\":\"a\"}"));
        store.setFailureInjector((method, args) -> "findForUpdate".equals(method)
                && String.valueOf(args[1]).equals(nodeA.buildStateDn(TASK))
                ? new EntryPersistenceException("state read failed") : null);
        assertFalse(nodeA.saveState(claim, current -> "{}"));
        store.setFailureInjector(null);
        assertEquals(nodeA.loadState(TASK), Optional.of("{\"by\":\"a\"}"));
    }

    @Test
    public void loadStateThrowsOnReadError() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        store.setFailureInjector((method, args) -> "find".equals(method) || "findForUpdate".equals(method)
                ? new EntryPersistenceException("Failed to find entry", new SearchException("query failed", new RuntimeException("io")))
                : null);
        expectThrows(EntryPersistenceException.class, () -> nodeA.loadState(TASK));
        assertFalse(nodeA.saveState(claim, current -> "{}"));
    }

    @Test
    public void dnLengthIsGuarded() {
        String task62 = repeat('t', 62);
        assertEquals(nodeA.buildClaimDn(task62, "1"), "jansId=" + task62 + "_1,ou=node,o=jans");
        expectThrows(IllegalArgumentException.class, () -> nodeA.buildClaimDn(task62, "12"));
        expectThrows(IllegalArgumentException.class, () -> nodeA.tryClaim(task62, "12", LEASE));
        expectThrows(IllegalArgumentException.class, () -> nodeA.buildStateDn(repeat('t', 59)));
        assertEquals(store.getCallCount("persist"), 0);

        FixedClusterLockService longBase = new FixedClusterLockService(store.getEntryManager(), "ou=" + repeat('b', 110) + ",o=jans",
                "key-c", "host-c", null, clock);
        expectThrows(IllegalArgumentException.class, () -> longBase.buildClaimDn("task", "1"));
    }

    @Test
    public void invalidCharactersAreRejected() {
        String[] badTasks = { null, "", "a,b", "a=b", "a b", "a/b", "a+b", "a\"b", "a\\b", "a;b", "a\u00e9", "a#b" };
        for (String task : badTasks) {
            expectThrows(IllegalArgumentException.class, () -> nodeA.tryClaim(task, "1", LEASE));
        }

        String[] badSlots = { null, "", "1_2", "1,2", "1=2", " 1", "state", "STATE", "State" };
        for (String slot : badSlots) {
            expectThrows(IllegalArgumentException.class, () -> nodeA.tryClaim(TASK, slot, LEASE));
        }

        expectThrows(IllegalArgumentException.class, () -> nodeA.tryClaim(TASK, "1", Duration.ZERO));
        expectThrows(IllegalArgumentException.class, () -> nodeA.tryClaim(TASK, "1", Duration.ofMillis(999)));
        expectThrows(IllegalArgumentException.class, () -> nodeA.tryClaim(TASK, "1", Duration.ofNanos(1)));
        expectThrows(IllegalArgumentException.class, () -> nodeA.tryClaim(TASK, "1", Duration.ofSeconds(-1)));
        expectThrows(IllegalArgumentException.class, () -> nodeA.tryClaim(TASK, "1", null));
        assertEquals(store.getCallCount("persist"), 0);

        assertTrue(nodeA.tryClaim("a.b-c_d", "2026-11-24T10.00Z", LEASE).isPresent());
        assertTrue(nodeA.tryClaim(TASK, "1", Duration.ofSeconds(1)).isPresent());
    }

    @Test
    public void expiredClaimCannotRenewOrWriteState() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        clock.advance(LEASE);
        assertFalse(nodeA.heartbeat(claim));
        assertFalse(nodeA.saveState(claim, current -> "{}"));
        assertFalse(nodeA.release(claim, ClusterLock.STATUS_DONE, null));
        assertEquals(store.getCallCount("merge"), 0);
    }

    @Test
    public void oldHandleCannotChangeReclaimedSlotOfSameService() {
        ClusterLock old = nodeA.tryClaim(TASK, "1", LEASE).get();
        clock.advance(LEASE);
        assertEquals(nodeA.purgeExpired(TASK, 1), 1);
        ClusterLock current = nodeA.tryClaim(TASK, "1", LEASE).get();
        assertFalse(old.getLockKey().equals(current.getLockKey()));
        assertFalse(nodeA.heartbeat(old));
        assertFalse(nodeA.release(old, ClusterLock.STATUS_DONE, null));
        assertFalse(nodeA.saveState(old, state -> "{}"));
        assertTrue(nodeA.heartbeat(current));
        assertTrue(nodeA.release(current, ClusterLock.STATUS_DONE, null));
    }

    @Test
    public void protectedWriteRollsBackIfLeaseExpiresBeforeCommit() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", LEASE).get();
        String target = nodeA.buildStateDn(TASK);
        expectThrows(IllegalStateException.class, () -> nodeA.withActiveClaim(claim, store.getEntryManager(), target, () -> {
            ClusterLock state = new ClusterLock();
            state.setDn(target);
            store.getEntryManager().persist(state);
            clock.advance(LEASE);
            return null;
        }));
        assertFalse(nodeA.loadState(TASK).isPresent());
    }

    @Test
    public void nonDuplicateFailureReturnsEmptyAndDoesNotThrow() {
        store.setFailureInjector((method, args) -> "persist".equals(method) ? InMemoryEntryManager.missingTable("dn") : null);

        assertFalse(nodeA.tryClaim(TASK, "1", LEASE).isPresent());
        assertFalse(nodeA.tryClaim(TASK, "2", LEASE).isPresent());
        assertFalse(ClusterLockService.isDuplicate(InMemoryEntryManager.missingTable("dn")));
    }

    @Test
    public void warningIsRateLimitedAndResetBySuccess() {
        ClusterLockService service = nodeA;
        String key = ClusterLockService.WARNING_PERSIST_FAILURE + "java.sql.SQLException";
        assertTrue(service.shouldWarn(key));
        assertFalse(service.shouldWarn(key));
        clock.advance(ClusterLockService.WARNING_INTERVAL.minusSeconds(1));
        assertFalse(service.shouldWarn(key));
        clock.advance(Duration.ofSeconds(1));
        assertTrue(service.shouldWarn(key));
        assertFalse(service.shouldWarn(key));

        String backendKey = ClusterLockService.WARNING_UNSUPPORTED_BACKEND + "spanner";
        assertTrue(service.shouldWarn(backendKey));

        assertTrue(nodeA.tryClaim(TASK, "1", LEASE).isPresent());

        assertTrue(service.shouldWarn(key));
        assertFalse(service.shouldWarn(backendKey));
    }

    @Test
    public void upsertBackendsAreRefusedBeforeInsert() {
        for (PersistenceEntryManager.PERSITENCE_TYPES type : new PersistenceEntryManager.PERSITENCE_TYPES[] {
                PersistenceEntryManager.PERSITENCE_TYPES.spanner, PersistenceEntryManager.PERSITENCE_TYPES.couchbase }) {
            store.setPersistenceType(type.name());

            assertFalse(nodeA.isClaimSupported());
            assertFalse(nodeA.tryClaim(TASK, "1", LEASE).isPresent());
            assertFalse(nodeB.tryClaim(TASK, "1", LEASE).isPresent());
        }
        assertEquals(store.getCallCount("persist"), 0);

        store.setPersistenceType(PersistenceEntryManager.PERSITENCE_TYPES.ldap.name());
        assertFalse(nodeA.tryClaim(TASK, "1", LEASE).isPresent());
        assertFalse(nodeB.tryClaim(TASK, "1", LEASE).isPresent());
    }

    @Test
    public void fakeUpsertModeOverwrites() {
        store.setPersistenceType(PersistenceEntryManager.PERSITENCE_TYPES.spanner.name());
        ClusterLock first = new ClusterLock();
        first.setDn("jansId=x,ou=node,o=jans");
        first.setLockKey("a");
        ClusterLock second = InMemoryEntryManager.copy(first);
        second.setLockKey("b");

        store.getEntryManager().persist(first);
        store.getEntryManager().persist(second);

        assertEquals(store.get(first.getDn(), ClusterLock.class).getLockKey(), "b");
    }

    @Test
    public void duplicateIsFoundAnywhereInCauseChain() {
        DuplicateEntryException duplicate = new DuplicateEntryException("dn");
        RuntimeException wrapped = new RuntimeException(new IllegalStateException(new EntryPersistenceException("x", duplicate)));
        assertTrue(ClusterLockService.isDuplicate(wrapped));
        assertTrue(ClusterLockService.isDuplicate(new EntryPersistenceException("Failed to persist entry", new DuplicateEntryException())));

        assertFalse(ClusterLockService.isDuplicate(new EntryPersistenceException("x", new IllegalStateException("y"))));
        assertFalse(ClusterLockService.isDuplicate(null));

        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);
        assertFalse(ClusterLockService.isDuplicate(first));
    }

    @Test
    public void missingEntryIsToldApartFromReadError() {
        assertTrue(ClusterLockService.isMissingEntry(InMemoryEntryManager.missing("dn")));
        assertFalse(ClusterLockService.isMissingEntry(new EntryPersistenceException("Failed to find entry: 'dn'")));
        assertFalse(ClusterLockService.isMissingEntry(
                new EntryPersistenceException("x", new SearchException("Failed to lookup query", new RuntimeException("io")))));
        assertFalse(ClusterLockService.isMissingEntry(new EntryPersistenceException("x", new SearchException("invalid query"))));
        assertFalse(ClusterLockService.isMissingEntry(new EntryPersistenceException("x", new RuntimeException("io"))));
    }

    @Test
    public void heartbeatKeepsUsingOriginalLease() {
        ClusterLock claim = nodeA.tryClaim(TASK, "1", Duration.ofSeconds(90)).get();
        clock.advance(Duration.ofSeconds(60));
        assertTrue(nodeA.heartbeat(claim));
        Instant later = clock.advance(Duration.ofSeconds(60));
        assertTrue(nodeA.heartbeat(claim));

        assertEquals(store.get(claim.getDn(), ClusterLock.class).getExpirationDate().toInstant(), later.plusSeconds(90));
    }

    @Test
    public void entryManagerIsTheInjectedFake() {
        assertSame(nodeA.getEntryManager(), store.getEntryManager());
    }

    private static String repeat(char c, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(c);
        }
        return result.toString();
    }

}

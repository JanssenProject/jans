/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TracePolicyReleaseEntry;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;

class OrmPolicyReleaseTest {

	private static final String HASH = "sha256:" + "a".repeat(64);

	@Test
	void findPolicyRelease_noMatch_empty() {
		PersistenceEntryManager manager = mock(PersistenceEntryManager.class);
		when(manager.findEntries(anyString(), any(Class.class), any(Filter.class), anyInt())).thenReturn(List.of());
		TraceStore store = OrmTraceStores.create(manager);

		assertFalse(store.findPolicyRelease("store-1", HASH).isPresent());
	}

	@Test
	void findPolicyRelease_backendFailure_throwsStorageException() {
		PersistenceEntryManager manager = mock(PersistenceEntryManager.class);
		when(manager.findEntries(anyString(), any(Class.class), any(Filter.class), anyInt()))
				.thenThrow(new EntryPersistenceException("Failed to find entries", new SQLException("connection lost")));
		TraceStore store = OrmTraceStores.create(manager);

		TraceStorageException ex = assertThrows(TraceStorageException.class,
				() -> store.findPolicyRelease("store-1", HASH));

		assertEquals("read_failed", ex.getReason());
	}

	@Test
	void findPolicyRelease_found_returnsEntry() {
		PersistenceEntryManager manager = mock(PersistenceEntryManager.class);
		TracePolicyReleaseEntry entry = new TracePolicyReleaseEntry();
		entry.setBundleHash(HASH);
		when(manager.findEntries(anyString(), any(Class.class), any(Filter.class), anyInt())).thenReturn(List.of(entry));
		TraceStore store = OrmTraceStores.create(manager);

		assertEquals(HASH, store.findPolicyRelease("store-1", HASH).orElseThrow().getBundleHash());
	}

	private static TracePolicyReleaseEntry release(long servedAtMs) {
		TracePolicyReleaseEntry entry = new TracePolicyReleaseEntry();
		entry.setPolicyStoreId("store-1");
		entry.setBundleHash(HASH);
		entry.setPolicyStoreVersion("1.0.0");
		entry.setFirstServedAtMs(servedAtMs);
		entry.setLastServedAtMs(servedAtMs);
		return entry;
	}

	@Test
	void recordPolicyReleaseServed_versionConflict_rereadsAndKeepsMaxAndMin() throws Exception {
		PersistenceEntryManager manager = FakeEntryManagerBehavior.create();
		TraceStore store = OrmTraceStores.create(manager);
		store.insertPolicyRelease(release(1_000L));
		FakeEntryManagerBehavior.injectVersionConflicts(manager, 1, stored -> {
			TracePolicyReleaseEntry raced = (TracePolicyReleaseEntry) stored;
			raced.setLastServedAtMs(3_000L);
			raced.setFirstServedAtMs(500L);
		});

		assertTrue(store.recordPolicyReleaseServed("store-1", HASH, "1.0.1", 5_000L));

		TracePolicyReleaseEntry found = store.findPolicyRelease("store-1", HASH).orElseThrow();
		assertEquals(5_000L, found.getLastServedAtMs());
		assertEquals(500L, found.getFirstServedAtMs());
		assertEquals("1.0.1", found.getPolicyStoreVersion());
	}

	@Test
	void recordPolicyReleaseServed_concurrentHigherValue_notLowered() throws Exception {
		PersistenceEntryManager manager = FakeEntryManagerBehavior.create();
		TraceStore store = OrmTraceStores.create(manager);
		store.insertPolicyRelease(release(1_000L));
		FakeEntryManagerBehavior.injectVersionConflicts(manager, 1,
				stored -> ((TracePolicyReleaseEntry) stored).setLastServedAtMs(9_000L));

		assertTrue(store.recordPolicyReleaseServed("store-1", HASH, "1.0.1", 5_000L));

		assertEquals(9_000L, store.findPolicyRelease("store-1", HASH).orElseThrow().getLastServedAtMs());
	}

	@Test
	void recordPolicyReleaseServed_conflictsExhausted_throwsVersionConflict() throws Exception {
		PersistenceEntryManager manager = FakeEntryManagerBehavior.create();
		TraceStore store = OrmTraceStores.create(manager);
		store.insertPolicyRelease(release(1_000L));
		long[] next = { 2_000L };
		FakeEntryManagerBehavior.injectVersionConflicts(manager, 3,
				stored -> ((TracePolicyReleaseEntry) stored).setLastServedAtMs(next[0]++));

		TraceStorageException ex = assertThrows(TraceStorageException.class,
				() -> store.recordPolicyReleaseServed("store-1", HASH, "1.0.1", 5_000L));

		assertEquals(TraceStore.VERSION_CONFLICT_REASON, ex.getReason());
	}

}

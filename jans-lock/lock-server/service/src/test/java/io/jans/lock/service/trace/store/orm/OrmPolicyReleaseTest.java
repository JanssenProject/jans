/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.entity.TracePolicyReleaseEntry;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;

class OrmPolicyReleaseTest {

	private static final String HASH = "sha256:" + "a".repeat(64);

	@Test
	void findPolicyRelease_missingDn_empty() {
		PersistenceEntryManager manager = mock(PersistenceEntryManager.class);
		when(manager.find(anyString(), any(Class.class), any()))
				.thenThrow(new EntryPersistenceException("Failed to find entry"));
		TraceStore store = OrmTraceStores.create(manager);

		assertFalse(store.findPolicyRelease("store-1", HASH).isPresent());
	}

	@Test
	void findPolicyRelease_backendFailure_throwsStorageException() {
		PersistenceEntryManager manager = mock(PersistenceEntryManager.class);
		when(manager.find(anyString(), any(Class.class), any()))
				.thenThrow(new EntryPersistenceException("Failed to find entry", new SQLException("connection lost")));
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
		when(manager.find(anyString(), any(Class.class), any())).thenReturn(entry);
		TraceStore store = OrmTraceStores.create(manager);

		assertEquals(HASH, store.findPolicyRelease("store-1", HASH).orElseThrow().getBundleHash());
	}

}

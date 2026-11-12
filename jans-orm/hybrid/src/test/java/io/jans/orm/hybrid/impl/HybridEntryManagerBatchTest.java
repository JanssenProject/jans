/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.hybrid.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.InvalidArgumentException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.UnsupportedOperationException;
import io.jans.orm.exception.operation.DuplicateEntryException;

/**
 * Unit tests for the batch API delegation of {@link HybridEntryManager}. No live DB: sub-managers are
 * Mockito mocks; DNs under {@code ou=people} map to sql, under {@code ou=tokens} to couchbase.
 */
public class HybridEntryManagerBatchTest {

	private final PersistenceEntryManager sqlDelegate = mock(PersistenceEntryManager.class);
	private final PersistenceEntryManager couchbaseDelegate = mock(PersistenceEntryManager.class);
	private final HybridEntryManager hybridEntryManager = buildHybridEntryManager();

	private HybridEntryManager buildHybridEntryManager() {
		Properties mappingProperties = new Properties();
		mappingProperties.setProperty("storage.default", "sql");
		mappingProperties.setProperty("storage.sql.mapping", "people");
		mappingProperties.setProperty("storage.couchbase.mapping", "tokens");

		HashMap<String, PersistenceEntryManager> persistenceEntryManagers = new HashMap<>();
		persistenceEntryManagers.put("sql", sqlDelegate);
		persistenceEntryManagers.put("couchbase", couchbaseDelegate);

		when(sqlDelegate.getPersistenceType()).thenReturn("sql");
		when(couchbaseDelegate.getPersistenceType()).thenReturn("couchbase");

		return new HybridEntryManager(mappingProperties, persistenceEntryManagers, null);
	}

	private static HybridTestEntry entry(String dn) {
		HybridTestEntry entry = new HybridTestEntry();
		entry.setDn(dn);
		return entry;
	}

	@Test
	public void isAtomicBatchSupportedForDnReportsCapabilityOfResolvedDelegate() {
		when(sqlDelegate.isAtomicBatchSupported("uid=a,ou=people,o=jans")).thenReturn(true);
		when(couchbaseDelegate.isAtomicBatchSupported("uid=t,ou=tokens,o=jans")).thenReturn(false);

		assertTrue(hybridEntryManager.isAtomicBatchSupported("uid=a,ou=people,o=jans"));
		assertFalse(hybridEntryManager.isAtomicBatchSupported("uid=t,ou=tokens,o=jans"));
	}

	@Test
	public void isAtomicBatchSupportedIsTrueOnlyWhenEveryDelegateSupportsIt() {
		when(sqlDelegate.isAtomicBatchSupported()).thenReturn(true);
		when(couchbaseDelegate.isAtomicBatchSupported()).thenReturn(false);
		assertFalse(hybridEntryManager.isAtomicBatchSupported());

		when(couchbaseDelegate.isAtomicBatchSupported()).thenReturn(true);
		assertTrue(hybridEntryManager.isAtomicBatchSupported());
	}

	@Test
	public void persistBatchDelegatesToBackendOfAllEntries() {
		List<Object> entries = Arrays.asList(entry("uid=a,ou=people,o=jans"), entry("uid=b,ou=people,o=jans"),
				entry("uid=c,ou=unmapped,o=jans"));

		hybridEntryManager.persistBatch(entries);

		verify(sqlDelegate).persistBatch(entries);
		verify(couchbaseDelegate, never()).persistBatch(any());
	}

	@Test
	public void persistBatchAcrossBackendsIsRejected() {
		List<Object> entries = Arrays.asList(entry("uid=a,ou=people,o=jans"), entry("uid=t,ou=tokens,o=jans"));

		InvalidArgumentException thrown = assertThrows(InvalidArgumentException.class,
				() -> hybridEntryManager.persistBatch(entries));

		assertTrue(thrown.getMessage().contains("'sql'"));
		assertTrue(thrown.getMessage().contains("'couchbase'"));
		verify(sqlDelegate, never()).persistBatch(any());
		verify(couchbaseDelegate, never()).persistBatch(any());
	}

	@Test
	public void persistBatchReportsUnsupportedDelegate() {
		List<Object> entries = Collections.singletonList(entry("uid=t,ou=tokens,o=jans"));
		UnsupportedOperationException notSupported = new UnsupportedOperationException(
				"Atomic batch insert (persistBatch) is not supported by 'couchbase' persistence backend");
		doThrow(notSupported).when(couchbaseDelegate).persistBatch(entries);

		UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
				() -> hybridEntryManager.persistBatch(entries));

		assertSame(notSupported, thrown);
	}

	@Test
	public void persistBatchPropagatesDuplicateFailureUnchanged() {
		List<Object> entries = Collections.singletonList(entry("uid=a,ou=people,o=jans"));
		EntryPersistenceException duplicate = new EntryPersistenceException("Failed to persist batch",
				new DuplicateEntryException("uid=a,ou=people,o=jans"));
		doThrow(duplicate).when(sqlDelegate).persistBatch(entries);

		EntryPersistenceException thrown = assertThrows(EntryPersistenceException.class,
				() -> hybridEntryManager.persistBatch(entries));

		assertSame(duplicate, thrown);
		assertTrue(thrown.getCause() instanceof DuplicateEntryException);
	}

	@Test
	public void persistBatchWithEmptyListIsNoOp() {
		hybridEntryManager.persistBatch(Collections.emptyList());

		verify(sqlDelegate, never()).persistBatch(any());
		verify(couchbaseDelegate, never()).persistBatch(any());
	}

	@Test
	public void persistBatchRejectsNullListAndNullElement() {
		assertThrows(MappingException.class, () -> hybridEntryManager.persistBatch(null));
		assertThrows(MappingException.class,
				() -> hybridEntryManager.persistBatch(Arrays.asList(entry("uid=a,ou=people,o=jans"), null)));
		verify(sqlDelegate, never()).persistBatch(any());
	}

	@Test
	public void findByDnsGroupsDnsByDelegateAndKeepsInputOrder() {
		HybridTestEntry person = entry("uid=a,ou=people,o=jans");
		HybridTestEntry token = entry("uid=t,ou=tokens,o=jans");
		when(sqlDelegate.findByDns(HybridTestEntry.class,
				Arrays.asList("uid=a,ou=people,o=jans", "uid=missing,ou=people,o=jans")))
				.thenReturn(Collections.singletonMap("uid=a,ou=people,o=jans", person));
		when(couchbaseDelegate.findByDns(HybridTestEntry.class, Collections.singletonList("uid=t,ou=tokens,o=jans")))
				.thenReturn(Collections.singletonMap("uid=t,ou=tokens,o=jans", token));

		Map<String, HybridTestEntry> found = hybridEntryManager.findByDns(HybridTestEntry.class,
				Arrays.asList("uid=t,ou=tokens,o=jans", "uid=a,ou=people,o=jans", "uid=missing,ou=people,o=jans"));

		assertEquals(Arrays.asList("uid=t,ou=tokens,o=jans", "uid=a,ou=people,o=jans"), new ArrayList<>(found.keySet()));
		assertSame(token, found.get("uid=t,ou=tokens,o=jans"));
		assertSame(person, found.get("uid=a,ou=people,o=jans"));
	}

	@Test
	public void findByDnsRejectsNullCollection() {
		assertThrows(MappingException.class, () -> hybridEntryManager.findByDns(HybridTestEntry.class, null));
	}

}

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;

/**
 * Tests the duplicate-detection protocol in isolation, with a plain Mockito mock of
 * {@link PersistenceEntryManager} rather than {@link FakeEntryManagerBehavior}.
 *
 * <p>On SQL the store does not read before an insert: the unique key decides and a duplicate arrives as an
 * ORM {@code DuplicateEntryException} cause. Any other persistence type (the mock's default {@code null}
 * included) can overwrite on insert, so the store probes {@code contains} first. A plain
 * {@link EntryPersistenceException} makes the store re-probe {@code contains} to tell a duplicate from a
 * genuine storage failure.</p>
 *
 * @author Yuriy Movchan
 */
class OrmTraceStoreDuplicateDetectionTest {

	private static final String DOMAIN = "default";

	@Mock
	private PersistenceEntryManager persistenceEntryManager;

	private OrmTraceStore store;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		store = OrmTraceStores.create(persistenceEntryManager);
	}

	private static TraceProducerKeyEntry newProducerKey() {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", "abc");
		TraceProducerKeyEntry key = new TraceProducerKeyEntry();
		key.setDomainId(DOMAIN);
		key.setProducerId("producer-1");
		key.setKid("kid-1");
		key.setPublicKeyJwk(jwk);
		key.setAuthorizedEventKinds(Collections.singletonList("AUTHORIZATION_DECISION"));
		key.setValidFrom(new Date(100L));
		key.setRegisteredBy("client-1");
		key.setCreationDate(new Date(100L));
		return key;
	}

	@Test
	void testInsertProducerKey_persistFailsAndContainsStaysFalse_throwsTraceStorageException() {
		when(persistenceEntryManager.contains(anyString(), eq(TraceProducerKeyEntry.class))).thenReturn(false);
		doThrow(new EntryPersistenceException("boom")).when(persistenceEntryManager).persist(any());

		assertThrows(TraceStorageException.class, () -> store.insertProducerKey(newProducerKey()));
	}

	@Test
	void testInsertProducerKey_persistFailsButContainsTrueOnReprobe_throwsDuplicateEntryException() {
		when(persistenceEntryManager.contains(anyString(), eq(TraceProducerKeyEntry.class))).thenReturn(false, true);
		doThrow(new EntryPersistenceException("boom")).when(persistenceEntryManager).persist(any());

		assertThrows(DuplicateEntryException.class, () -> store.insertProducerKey(newProducerKey()));
	}

	@Test
	void testInsertProducerKey_containsTrueUpfront_throwsDuplicateEntryExceptionWithoutPersisting() {
		when(persistenceEntryManager.contains(anyString(), eq(TraceProducerKeyEntry.class))).thenReturn(true);

		assertThrows(DuplicateEntryException.class, () -> store.insertProducerKey(newProducerKey()));

		verify(persistenceEntryManager, never()).persist(any());
	}

	@Test
	void testInsertProducerKey_couchbaseExistingEntry_isNotOverwritten() {
		when(persistenceEntryManager.getPersistenceType(anyString())).thenReturn(PersistenceEntryManager.PERSITENCE_TYPES.couchbase.name());
		when(persistenceEntryManager.contains(anyString(), eq(TraceProducerKeyEntry.class))).thenReturn(true);

		assertThrows(DuplicateEntryException.class, () -> store.insertProducerKey(newProducerKey()));

		verify(persistenceEntryManager, never()).persist(any());
	}

	@Test
	void testInsertProducerKey_sqlPersistFailsWithDuplicateCause_throwsDuplicateEntryExceptionWithoutRead() {
		when(persistenceEntryManager.getPersistenceType(anyString())).thenReturn(PersistenceEntryManager.PERSITENCE_TYPES.sql.name());
		doThrow(new EntryPersistenceException("Failed to persist entry",
				new io.jans.orm.exception.operation.DuplicateEntryException("dn"))).when(persistenceEntryManager).persist(any());

		assertThrows(DuplicateEntryException.class, () -> store.insertProducerKey(newProducerKey()));

		verify(persistenceEntryManager, never()).contains(anyString(), any(Class.class));
	}

	@Test
	void testInsertProducerKey_sqlPersistFailsWithoutCauseAndContainsTrue_throwsDuplicateEntryException() {
		when(persistenceEntryManager.getPersistenceType(anyString())).thenReturn(PersistenceEntryManager.PERSITENCE_TYPES.sql.name());
		when(persistenceEntryManager.contains(anyString(), eq(TraceProducerKeyEntry.class))).thenReturn(true);
		doThrow(new EntryPersistenceException("boom")).when(persistenceEntryManager).persist(any());

		assertThrows(DuplicateEntryException.class, () -> store.insertProducerKey(newProducerKey()));

		verify(persistenceEntryManager).contains(anyString(), eq(TraceProducerKeyEntry.class));
	}

	@Test
	void testInsertProducerKey_sqlPersistSucceeds_readsNothing() throws Exception {
		when(persistenceEntryManager.getPersistenceType(anyString())).thenReturn(PersistenceEntryManager.PERSITENCE_TYPES.sql.name());

		store.insertProducerKey(newProducerKey());

		verify(persistenceEntryManager).persist(any());
		verify(persistenceEntryManager, never()).contains(anyString(), any(Class.class));
	}

}

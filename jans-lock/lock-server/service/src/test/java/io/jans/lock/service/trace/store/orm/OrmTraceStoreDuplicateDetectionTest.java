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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.BaseDnConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ProducerKey;
import io.jans.lock.service.trace.store.DuplicateEntryException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;

/**
 * Tests the backend-agnostic duplicate-detection protocol (design decision D-8) in isolation, with
 * a plain Mockito mock of {@link PersistenceEntryManager} rather than {@link FakeEntryManagerBehavior}:
 * {@code persist} throws a plain {@link EntryPersistenceException} — never
 * {@code io.jans.orm.exception.operation.DuplicateEntryException} in its cause chain, exactly as
 * the SQL backend does — and the store must re-probe {@code contains} to tell a real race from a
 * genuine storage failure.
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

		BaseDnConfiguration baseDnConfiguration = new BaseDnConfiguration();
		baseDnConfiguration.setTrace("ou=trace,ou=lock,o=jans");
		StaticConfiguration staticConfiguration = new StaticConfiguration();
		staticConfiguration.setBaseDn(baseDnConfiguration);

		store = new OrmTraceStore();
		store.setLog(LoggerFactory.getLogger(OrmTraceStore.class));
		store.setPersistenceEntryManager(persistenceEntryManager);
		store.setStaticConfiguration(staticConfiguration);
	}

	private static ProducerKey newProducerKey() {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", "abc");
		return new ProducerKey(DOMAIN, "producer-1", "kid-1", jwk, 100L, null, null, "client-1");
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

}

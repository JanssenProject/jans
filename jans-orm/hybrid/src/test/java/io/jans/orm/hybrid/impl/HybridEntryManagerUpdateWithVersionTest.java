/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.hybrid.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.UnsupportedOperationException;
import io.jans.orm.exception.VersionMismatchException;

/**
 * Unit tests for {@link HybridEntryManager#updateWithVersion(Object)} -- task 06. No live DB: the
 * delegate resolution ({@code getEntryManagerForDn}) is exercised against mocked
 * {@link PersistenceEntryManager} sub-managers, same as {@code merge}/{@code persist} already do.
 */
public class HybridEntryManagerUpdateWithVersionTest {

	private HybridEntryManager buildHybridEntryManager(PersistenceEntryManager sqlDelegate,
			PersistenceEntryManager couchbaseDelegate) {
		Properties mappingProperties = new Properties();
		mappingProperties.setProperty("storage.default", "sql");
		mappingProperties.setProperty("storage.sql.mapping", "people");
		mappingProperties.setProperty("storage.couchbase.mapping", "tokens");

		HashMap<String, PersistenceEntryManager> persistenceEntryManagers = new HashMap<>();
		persistenceEntryManagers.put("sql", sqlDelegate);
		persistenceEntryManagers.put("couchbase", couchbaseDelegate);

		return new HybridEntryManager(mappingProperties, persistenceEntryManagers, null);
	}

	@Test
	public void updateWithVersionDelegatesToSubManagerResolvedByDn() {
		PersistenceEntryManager sqlDelegate = mock(PersistenceEntryManager.class);
		PersistenceEntryManager couchbaseDelegate = mock(PersistenceEntryManager.class);
		HybridEntryManager hybridEntryManager = buildHybridEntryManager(sqlDelegate, couchbaseDelegate);

		HybridTestEntry entry = new HybridTestEntry();
		entry.setDn("uid=test1,ou=people,o=jans");

		doNothing().when(sqlDelegate).updateWithVersion(entry);

		hybridEntryManager.updateWithVersion(entry);

		verify(sqlDelegate).updateWithVersion(entry);
	}

	@Test
	public void updateWithVersionPropagatesUnsupportedOperationExceptionFromNotYetCasCapableDelegate() {
		PersistenceEntryManager sqlDelegate = mock(PersistenceEntryManager.class);
		PersistenceEntryManager couchbaseDelegate = mock(PersistenceEntryManager.class);
		HybridEntryManager hybridEntryManager = buildHybridEntryManager(sqlDelegate, couchbaseDelegate);

		HybridTestEntry entry = new HybridTestEntry();
		entry.setDn("uid=test2,ou=tokens,o=jans");

		UnsupportedOperationException notSupported = new UnsupportedOperationException(
				"Versioned update (updateWithVersion) is not supported by 'couchbase' persistence backend");
		doThrow(notSupported).when(couchbaseDelegate).updateWithVersion(entry);

		UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
				() -> hybridEntryManager.updateWithVersion(entry));
		assertSame(notSupported, thrown);
	}

	@Test
	public void updateWithVersionPropagatesVersionMismatchExceptionFromDelegate() {
		PersistenceEntryManager sqlDelegate = mock(PersistenceEntryManager.class);
		PersistenceEntryManager couchbaseDelegate = mock(PersistenceEntryManager.class);
		HybridEntryManager hybridEntryManager = buildHybridEntryManager(sqlDelegate, couchbaseDelegate);

		HybridTestEntry entry = new HybridTestEntry();
		entry.setDn("uid=test3,ou=people,o=jans");

		VersionMismatchException staleVersion = new VersionMismatchException(entry.getDn(), 5L);
		doThrow(staleVersion).when(sqlDelegate).updateWithVersion(entry);

		VersionMismatchException thrown = assertThrows(VersionMismatchException.class,
				() -> hybridEntryManager.updateWithVersion(entry));
		assertSame(staleVersion, thrown);
	}

	@Test
	public void updateWithVersionOnUnmappedBaseFallsBackToDefaultDelegate() {
		PersistenceEntryManager sqlDelegate = mock(PersistenceEntryManager.class);
		PersistenceEntryManager couchbaseDelegate = mock(PersistenceEntryManager.class);
		HybridEntryManager hybridEntryManager = buildHybridEntryManager(sqlDelegate, couchbaseDelegate);

		HybridTestEntry entry = new HybridTestEntry();
		// "unmapped" has no storage.<type>.mapping entry -- must fall back to storage.default (sql)
		entry.setDn("uid=test4,ou=unmapped,o=jans");

		doNothing().when(sqlDelegate).updateWithVersion(entry);

		hybridEntryManager.updateWithVersion(entry);

		verify(sqlDelegate).updateWithVersion(entry);
	}

}

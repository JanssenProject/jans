/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.UnsupportedOperationException;
import io.jans.orm.impl.model.PreparedInsert;
import io.jans.orm.model.AttributeData;

/**
 * Unit tests for the default (non-SQL) behavior of the batch API in
 * {@link io.jans.orm.PersistenceEntryManager} and for {@link BaseEntryManager#prepareInsert(Object)}.
 */
public class PersistenceEntryManagerBatchDefaultsTest {

	private static BatchTestEntry entry(String dn, String uid) {
		BatchTestEntry entry = new BatchTestEntry();
		entry.setDn(dn);
		entry.setUid(uid);
		return entry;
	}

	@Test
	public void atomicBatchIsNotSupportedByDefault() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();

		assertFalse(entryManager.isAtomicBatchSupported());
		assertFalse(entryManager.isAtomicBatchSupported("uid=a,o=test"));
	}

	@Test
	public void persistBatchThrowsUnsupportedByDefaultAndStoresNothing() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();
		List<Object> entries = Arrays.asList(entry("uid=a,o=test", "a"), entry("uid=b,o=test", "b"));

		UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
				() -> entryManager.persistBatch(entries));

		assertTrue(thrown.getMessage().contains("'test'"));
		assertTrue(entryManager.storage.isEmpty());
	}

	@Test
	public void persistBatchThrowsUnsupportedByDefaultForEmptyList() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();

		assertThrows(UnsupportedOperationException.class, () -> entryManager.persistBatch(Collections.emptyList()));
	}

	@Test
	public void findByDnsReturnsFoundEntriesAndOmitsMissingOnes() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();
		entryManager.persist(entry("uid=a,o=test", "a"));
		entryManager.persist(entry("uid=c,o=test", "c"));

		Map<String, BatchTestEntry> found = entryManager.findByDns(BatchTestEntry.class,
				Arrays.asList("uid=c,o=test", "uid=missing,o=test", "uid=a,o=test", "uid=c,o=test"));

		assertEquals(Arrays.asList("uid=c,o=test", "uid=a,o=test"), new ArrayList<>(found.keySet()));
		assertEquals("c", found.get("uid=c,o=test").getUid());
		assertEquals("a", found.get("uid=a,o=test").getUid());
		assertEquals("uid=a,o=test", found.get("uid=a,o=test").getDn());
	}

	@Test
	public void findByDnsWithEmptyCollectionReturnsEmptyMap() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();

		assertTrue(entryManager.findByDns(BatchTestEntry.class, Collections.emptyList()).isEmpty());
	}

	@Test
	public void findByDnsRejectsNullCollectionAndNullDn() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();

		assertThrows(MappingException.class, () -> entryManager.findByDns(BatchTestEntry.class, null));
		assertThrows(MappingException.class,
				() -> entryManager.findByDns(BatchTestEntry.class, Arrays.asList("uid=a,o=test", null)));
	}

	@Test
	public void findByDnsSkipsEntryRemovedBetweenContainsAndFind() {
		RacingFindEntryManager entryManager = new RacingFindEntryManager();
		entryManager.containsAnswers = new ArrayList<>(Arrays.asList(true, false));

		Map<String, BatchTestEntry> found = entryManager.findByDns(BatchTestEntry.class,
				Collections.singletonList("uid=a,o=test"));

		assertTrue(found.isEmpty());
	}

	@Test
	public void findByDnsPropagatesFindFailureWhenEntryStillExists() {
		RacingFindEntryManager entryManager = new RacingFindEntryManager();
		entryManager.containsAnswers = new ArrayList<>(Arrays.asList(true, true));

		EntryPersistenceException thrown = assertThrows(EntryPersistenceException.class,
				() -> entryManager.findByDns(BatchTestEntry.class, Collections.singletonList("uid=a,o=test")));

		assertSame(entryManager.findFailure, thrown);
	}

	@Test
	public void prepareInsertMapsEntryLikePersist() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();
		BatchTestEntry entry = entry("uid=a,o=test", "a");

		PreparedInsert preparedInsert = entryManager.prepareInsert(entry);

		assertEquals("uid=a,o=test", preparedInsert.getDn());
		assertEquals(Arrays.asList("jansTestEntry"), Arrays.asList(preparedInsert.getObjectClasses()));
		assertEquals(null, preparedInsert.getExpiration());
		assertEquals(0L, entry.getVersion());

		FakeVersionEntryManager persistEntryManager = new FakeVersionEntryManager();
		persistEntryManager.persist(entry("uid=a,o=test", "a"));
		assertEquals(attributeNames(persistEntryManager.storage.get("uid=a,o=test")),
				attributeNames(preparedInsert.getAttributes()));
		assertTrue(attributeNames(preparedInsert.getAttributes()).contains("objectClass"));
	}

	@Test
	public void prepareInsertResultIsImmutable() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();
		PreparedInsert preparedInsert = entryManager.prepareInsert(entry("uid=a,o=test", "a"));

		assertThrows(java.lang.UnsupportedOperationException.class,
				() -> preparedInsert.getAttributes().add(new AttributeData("x", "y")));
		preparedInsert.getObjectClasses()[0] = "changed";
		assertEquals("jansTestEntry", preparedInsert.getObjectClasses()[0]);
	}

	@Test
	public void prepareInsertRejectsNullEntry() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();

		assertThrows(MappingException.class, () -> entryManager.prepareInsert(null));
	}

	private static List<String> attributeNames(List<AttributeData> attributes) {
		List<String> names = new ArrayList<>();
		for (AttributeData attribute : attributes) {
			names.add(attribute.getName());
		}
		return names;
	}

	/**
	 * Answers {@code contains} from a script and always fails {@code find}, to simulate an entry
	 * removed (or a read failure) between the two calls of the default {@code findByDns}.
	 */
	static class RacingFindEntryManager extends FakeVersionEntryManager {

		List<Boolean> containsAnswers;
		final EntryPersistenceException findFailure = new EntryPersistenceException("Failed to find entry");

		@Override
		public <T> boolean contains(String primaryKey, Class<T> entryClass) {
			return containsAnswers.remove(0);
		}

		@Override
		public <T> T find(Object primaryKey, Class<T> entryClass, String[] ldapReturnAttributes) {
			throw findFailure;
		}

	}

}

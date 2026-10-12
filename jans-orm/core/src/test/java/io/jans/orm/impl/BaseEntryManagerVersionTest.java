/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jans.orm.exception.MappingException;
import io.jans.orm.exception.UnsupportedOperationException;
import io.jans.orm.exception.VersionMismatchException;
import io.jans.orm.impl.VersionTestFixtures.DoubleVersionEntry;
import io.jans.orm.impl.VersionTestFixtures.ForceUpdateVersionedEntry;
import io.jans.orm.impl.VersionTestFixtures.NoVersionEntry;
import io.jans.orm.impl.VersionTestFixtures.VersionedEntry;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.AttributeDataModification.AttributeModificationType;

/**
 * Unit tests for the {@code @Version} / {@code updateWithVersion} CAS foundation added to
 * {@link BaseEntryManager}. Pure unit tests against in-memory fakes -- no live DB.
 */
public class BaseEntryManagerVersionTest {

	@Test
	public void mergeOnVersionedEntityBumpsVersionEvenWithNoOtherChange() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();
		entryManager.storage.put("o=test", List.of(
				new AttributeData("uid", "same"),
				new AttributeData("jansversion", "5")));

		VersionedEntry entry = new VersionedEntry();
		entry.setDn("o=test");
		entry.setUid("same");
		entry.setVersion(5L);

		entryManager.merge(entry);

		List<AttributeDataModification> modifications = entryManager.lastMergeModifications;
		assertEquals(1, modifications.size());

		AttributeDataModification versionModification = modifications.get(0);
		assertEquals(AttributeModificationType.REPLACE, versionModification.getModificationType());
		assertEquals("jansversion", versionModification.getAttribute().getName());
		assertEquals(6L, versionModification.getAttribute().getValue());
	}

	@Test
	public void mergeOnVersionedEntityBumpsFromDbValueNotInMemoryValue() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();
		entryManager.storage.put("o=test", List.of(
				new AttributeData("uid", "same"),
				new AttributeData("jansversion", "5")));

		VersionedEntry entry = new VersionedEntry();
		entry.setDn("o=test");
		entry.setUid("same");
		// Stale in-memory value -- merge() must bump off the DB value (5), not this one.
		entry.setVersion(3L);

		entryManager.merge(entry);

		List<AttributeDataModification> modifications = entryManager.lastMergeModifications;
		assertEquals(1, modifications.size());
		assertEquals(6L, modifications.get(0).getAttribute().getValue());
	}

	@Test
	public void updateWithVersionOnEntityWithTwoVersionFieldsThrowsMappingException() {
		VersioningFakeVersionEntryManager entryManager = new VersioningFakeVersionEntryManager();

		DoubleVersionEntry entry = new DoubleVersionEntry();
		entry.setDn("o=test");
		entry.setVersion(1L);
		entry.setVersion2(1L);

		MappingException ex = assertThrows(MappingException.class, () -> entryManager.updateWithVersion(entry));
		assertTrue(ex.getMessage().contains(DoubleVersionEntry.class.getName()));
	}

	@Test
	public void updateWithVersionOnEntityWithZeroVersionFieldsThrowsUnsupportedOperationException() {
		VersioningFakeVersionEntryManager entryManager = new VersioningFakeVersionEntryManager();

		NoVersionEntry entry = new NoVersionEntry();
		entry.setDn("o=test");
		entry.setUid("same");

		UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
				() -> entryManager.updateWithVersion(entry));
		assertTrue(ex.getMessage().contains(NoVersionEntry.class.getName()));
	}

	@Test
	public void updateWithVersionWithNullVersionFieldThrowsMappingException() {
		VersioningFakeVersionEntryManager entryManager = new VersioningFakeVersionEntryManager();

		VersionedEntry entry = new VersionedEntry();
		entry.setDn("o=test");
		entry.setUid("same");
		entry.setVersion(null);

		assertThrows(MappingException.class, () -> entryManager.updateWithVersion(entry));
	}

	@Test
	public void updateWithVersionAgainstDefaultMergeWithVersionHookThrowsUnsupportedOperationException() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();
		entryManager.storage.put("o=test", new ArrayList<>(List.of(
				new AttributeData("uid", "same"),
				new AttributeData("jansversion", "5"))));

		VersionedEntry entry = new VersionedEntry();
		entry.setDn("o=test");
		entry.setUid("same");
		entry.setVersion(5L);

		UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
				() -> entryManager.updateWithVersion(entry));
		assertTrue(ex.getMessage().contains(entryManager.getPersistenceType()));
	}

	@Test
	public void updateWithVersionSucceedsAndWritesNewVersionBackOntoEntry() {
		VersioningFakeVersionEntryManager entryManager = new VersioningFakeVersionEntryManager();
		entryManager.mergeWithVersionResult = true;
		entryManager.storage.put("o=test", new ArrayList<>(List.of(
				new AttributeData("uid", "same"),
				new AttributeData("jansversion", "5"))));

		VersionedEntry entry = new VersionedEntry();
		entry.setDn("o=test");
		entry.setUid("same");
		entry.setVersion(5L);

		entryManager.updateWithVersion(entry);

		assertEquals(Long.valueOf(6L), entry.getVersion());
		assertEquals("jansversion", entryManager.lastVersionAttributeName);
		assertEquals(Long.valueOf(5L), entryManager.lastExpectedVersionValue);
		assertEquals(Long.valueOf(6L), entryManager.lastNewVersionValue);
	}

	@Test
	public void updateWithVersionOnStaleVersionThrowsVersionMismatchException() {
		VersioningFakeVersionEntryManager entryManager = new VersioningFakeVersionEntryManager();
		entryManager.mergeWithVersionResult = false;
		entryManager.storage.put("o=test", new ArrayList<>(List.of(
				new AttributeData("uid", "same"),
				new AttributeData("jansversion", "5"))));

		VersionedEntry entry = new VersionedEntry();
		entry.setDn("o=test");
		entry.setUid("same");
		entry.setVersion(5L);

		VersionMismatchException ex = assertThrows(VersionMismatchException.class,
				() -> entryManager.updateWithVersion(entry));
		assertEquals("o=test", ex.getDn());
		assertEquals(Long.valueOf(5L), ex.getExpectedVersion());

		// Entry's in-memory version is left untouched on a rejected CAS
		assertEquals(Long.valueOf(5L), entry.getVersion());
	}

	@Test
	public void mergeOnForceUpdateVersionedEntityThrowsMappingException() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();

		ForceUpdateVersionedEntry entry = new ForceUpdateVersionedEntry();
		entry.setDn("o=test");
		entry.setUid("same");
		entry.setVersion(5L);

		assertThrows(MappingException.class, () -> entryManager.merge(entry));
	}

}

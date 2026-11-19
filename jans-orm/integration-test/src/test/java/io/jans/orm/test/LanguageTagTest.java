/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.test.model.SimpleLocalizedEntry;

/**
 * Attributes with both {@code @JsonObject} and {@code @LanguageTag} annotations:
 * values must round trip, must not leak into {@code @AttributesList} on load and
 * a loaded entry must be persistable again (no duplicate columns).
 *
 * Table required for this test (PostgreSQL), created by linux-setup from
 * jans_test_schema.json (objectClass jansTestLocalized):
 *
 * CREATE TABLE "jansTestLocalized" (doc_id VARCHAR(64) NOT NULL UNIQUE,
 *     "objectClass" VARCHAR(48), dn VARCHAR(128), "displayName" VARCHAR(128),
 *     "displayNameLocalized" JSONB, "jansClntURI" TEXT, "jansClntURILocalized" JSONB,
 *     "jansDataStr" TEXT, PRIMARY KEY (doc_id));
 *
 * Test is skipped when DB has no jansTestLocalized table.
 *
 * @author Yuriy Movchan
 */
public class LanguageTagTest extends BaseOrmTest {

	private static final String TABLE = "jansTestLocalized";
	private static final String CUSTOM_ATTRIBUTE = "jansDataStr";
	private static final String URI = "https://example.org";
	private static final String URI_EN = "https://example.org/en";
	private static final String URI_FR = "https://example.org/fr";

	private final List<String> createdDns = new ArrayList<>();

	private SimpleLocalizedEntry persistedEntry;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if (entryManager == null) {
			return;
		}

		for (String dn : createdDns) {
			try {
				entryManager.remove(dn, SimpleLocalizedEntry.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createEntryWithLocalizedAttributes() {
		requirePersistenceType(SQL);
		requireTable();

		SimpleLocalizedEntry newEntry = new SimpleLocalizedEntry();
		newEntry.setDn(newDn());
		newEntry.setDisplayName("Localized test");
		newEntry.getDisplayNameLocalized().setValue("Localized test EN", Locale.ENGLISH);
		newEntry.setClientUri(URI);
		newEntry.getClientUriLocalized().setValue(URI_EN, Locale.ENGLISH);
		newEntry.getClientUriLocalized().setValue(URI_FR, Locale.FRENCH);
		newEntry.getCustomAttributes().add(new CustomObjectAttribute(CUSTOM_ATTRIBUTE, "custom value"));

		entryManager.persist(newEntry);

		persistedEntry = newEntry;
	}

	@Test(dependsOnMethods = "createEntryWithLocalizedAttributes")
	public void readLocalizedAttributes() {
		SimpleLocalizedEntry foundEntry = entryManager.find(SimpleLocalizedEntry.class, persistedEntry.getDn());

		assertNotNull(foundEntry);
		assertEquals(foundEntry.getDisplayName(), "Localized test");
		assertEquals(foundEntry.getDisplayNameLocalized().getValue(Locale.ENGLISH), "Localized test EN");
		assertEquals(foundEntry.getClientUri(), URI);
		assertEquals(foundEntry.getClientUriLocalized().getValue(Locale.ENGLISH), URI_EN);
		assertEquals(foundEntry.getClientUriLocalized().getValue(Locale.FRENCH), URI_FR);
	}

	@Test(dependsOnMethods = "readLocalizedAttributes")
	public void localizedAttributesAreNotInCustomAttributes() {
		SimpleLocalizedEntry foundEntry = entryManager.find(SimpleLocalizedEntry.class, persistedEntry.getDn());

		List<String> names = new ArrayList<>();
		for (CustomObjectAttribute attribute : foundEntry.getCustomAttributes()) {
			names.add(attribute.getName());
		}

		assertEquals(names, List.of(CUSTOM_ATTRIBUTE), "Only custom attribute is expected: " + names);
	}

	@Test(dependsOnMethods = "localizedAttributesAreNotInCustomAttributes")
	public void persistLoadedEntryUnderNewDn() {
		SimpleLocalizedEntry foundEntry = entryManager.find(SimpleLocalizedEntry.class, persistedEntry.getDn());
		foundEntry.setDn(newDn());

		// Failed with: column "jansClntURILocalized" specified more than once
		entryManager.persist(foundEntry);

		SimpleLocalizedEntry copy = entryManager.find(SimpleLocalizedEntry.class, foundEntry.getDn());
		assertNotNull(copy);
		assertEquals(copy.getClientUri(), URI);
		assertEquals(copy.getClientUriLocalized().getValue(Locale.ENGLISH), URI_EN);
		assertEquals(copy.getClientUriLocalized().getValue(Locale.FRENCH), URI_FR);
		assertEquals(copy.getCustomAttributes().size(), 1);
	}

	@Test(dependsOnMethods = "localizedAttributesAreNotInCustomAttributes")
	public void mergeLoadedEntry() {
		SimpleLocalizedEntry foundEntry = entryManager.find(SimpleLocalizedEntry.class, persistedEntry.getDn());
		foundEntry.getClientUriLocalized().setValue("https://example.org/de", Locale.GERMAN);
		foundEntry.getDisplayNameLocalized().setValue("Localized test FR", Locale.FRENCH);

		entryManager.merge(foundEntry);

		SimpleLocalizedEntry updatedEntry = entryManager.find(SimpleLocalizedEntry.class, persistedEntry.getDn());
		assertEquals(updatedEntry.getClientUriLocalized().getValue(Locale.GERMAN), "https://example.org/de");
		assertEquals(updatedEntry.getClientUriLocalized().getValue(Locale.ENGLISH), URI_EN);
		assertEquals(updatedEntry.getDisplayNameLocalized().getValue(Locale.FRENCH), "Localized test FR");
		assertEquals(updatedEntry.getCustomAttributes().size(), 1);
	}

	@Test(dependsOnMethods = "createEntryWithLocalizedAttributes")
	public void emptyLocalizedAttributes() {
		SimpleLocalizedEntry newEntry = new SimpleLocalizedEntry();
		newEntry.setDn(newDn());
		newEntry.setDisplayName("Empty localized test");

		entryManager.persist(newEntry);

		SimpleLocalizedEntry foundEntry = entryManager.find(SimpleLocalizedEntry.class, newEntry.getDn());
		assertNotNull(foundEntry);
		assertEquals(foundEntry.getDisplayName(), "Empty localized test");
		assertEquals(foundEntry.getDisplayNameLocalized().size(), 0);
		assertEquals(foundEntry.getClientUriLocalized().size(), 0);
		assertNull(foundEntry.getClientUri());
		assertEquals(foundEntry.getCustomAttributes().size(), 0);

		// Loaded entry with empty localized values should be persistable as well
		foundEntry.setDn(newDn());
		entryManager.persist(foundEntry);
	}

	private String newDn() {
		String dn = String.format("inum=%s,ou=localized,o=jans", getRandomInum());
		createdDns.add(dn);
		return dn;
	}

	private void requireTable() {
		if (!entryManager.getTableColumnsMap().containsKey(TABLE.toLowerCase())) {
			throw new SkipException("DB has no " + TABLE + " table. See test javadoc for DDL");
		}
	}

}

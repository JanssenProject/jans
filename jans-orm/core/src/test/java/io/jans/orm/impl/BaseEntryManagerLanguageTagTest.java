/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.jans.orm.model.AttributeData;
import io.jans.orm.model.base.CustomObjectAttribute;

/**
 * {@code @LanguageTag} attributes must not leak into {@code @AttributesList} on load,
 * otherwise re-persisting the loaded entry emits the same attribute twice.
 */
public class BaseEntryManagerLanguageTagTest {

	private static final String DN = "inum=1,ou=test,o=jans";

	@Test
	public void loadedLocalizedAttributeDoesNotLeakIntoCustomAttributes() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();

		LocalizedEntry entry = new LocalizedEntry();
		entry.setDn(DN);
		entry.setUri("https://example.org");
		entry.getUriLocalized().setValue("https://example.org/en", Locale.ENGLISH);
		entry.getUriLocalized().setValue("https://example.org/fr", Locale.FRENCH);
		entry.getCustomAttributes().add(customAttribute("custom", "value"));

		entryManager.persist(entry);

		LocalizedEntry loaded = entryManager.find(DN, LocalizedEntry.class, null);

		assertEquals("https://example.org", loaded.getUri());
		assertEquals("https://example.org/en", loaded.getUriLocalized().getValue(Locale.ENGLISH));
		assertEquals("https://example.org/fr", loaded.getUriLocalized().getValue(Locale.FRENCH));
		assertEquals(1, loaded.getCustomAttributes().size());
		assertEquals("custom", loaded.getCustomAttributes().get(0).getName());
	}

	@Test
	public void rePersistOfLoadedEntryHasNoDuplicateAttributes() {
		FakeVersionEntryManager entryManager = new FakeVersionEntryManager();

		LocalizedEntry entry = new LocalizedEntry();
		entry.setDn(DN);
		entry.setUri("https://example.org");
		entry.getUriLocalized().setValue("https://example.org/en", Locale.ENGLISH);

		entryManager.persist(entry);
		LocalizedEntry loaded = entryManager.find(DN, LocalizedEntry.class, null);
		entryManager.persist(loaded);

		List<String> names = new ArrayList<>();
		for (AttributeData attribute : entryManager.storage.get(DN)) {
			names.add(attribute.getName().toLowerCase());
		}
		Set<String> unique = new HashSet<>(names);
		assertEquals(unique.size(), names.size(), "Duplicate attributes: " + names);
		assertTrue(names.contains("jansurilocalized"));
	}

	@Test
	public void ldapStyleLoadKeepsRegularAttributeAndHidesLocalizedOnes() {
		FakeLdapStyleEntryManager entryManager = new FakeLdapStyleEntryManager();
		entryManager.storage.put(DN, List.of(
				new AttributeData("displayName", "Default name"),
				new AttributeData("displayName;lang-en", "English name"),
				new AttributeData("jansUri", "https://example.org"),
				new AttributeData("jansUri;lang-fr", "https://example.org/fr"),
				new AttributeData("jansUriLocalized", "unrelated schema attribute"),
				new AttributeData("custom", "value")));

		LdapStyleLocalizedEntry loaded = entryManager.find(DN, LdapStyleLocalizedEntry.class, null);

		// Regular property keeps its value regardless of properties order
		assertEquals("Default name", loaded.getDisplayName());
		assertEquals("Default name", loaded.getDisplayNameLocalized().getValue());
		assertEquals("English name", loaded.getDisplayNameLocalized().getValue(Locale.ENGLISH));
		assertEquals("https://example.org", loaded.getUriLocalized().getValue());
		assertEquals("https://example.org/fr", loaded.getUriLocalized().getValue(Locale.FRENCH));

		List<String> names = new ArrayList<>();
		for (CustomObjectAttribute attribute : loaded.getCustomAttributes()) {
			names.add(attribute.getName());
		}
		// jansUri (no regular property) and jansUri;lang-fr are consumed, jansUriLocalized is a distinct attribute
		assertEquals(List.of("custom", "jansUriLocalized"), names);
	}

	private static CustomObjectAttribute customAttribute(String name, String value) {
		CustomObjectAttribute attribute = new CustomObjectAttribute(name, value);
		return attribute;
	}
}

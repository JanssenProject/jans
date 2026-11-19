/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.impl;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import io.jans.orm.model.AttributeData;
import io.jans.orm.model.base.LocalizedString;

/**
 * In-memory manager which stores localized values like LDAP does: {@code attr} for default value
 * and {@code attr;lang-xx} for others. Mirrors LdapEntryManager hooks to test shared load logic.
 */
class FakeLdapStyleEntryManager extends FakeVersionEntryManager {

	@Override
	protected Map<String, AttributeData> filterLocalizedStringAttributes(Map<String, AttributeData> attributesMap, String ldapAttributeName) {
		final String name = ldapAttributeName.replace(LocalizedString.LOCALIZED, LocalizedString.EMPTY_LANG_TAG).toLowerCase();
		return attributesMap.entrySet().stream()
				.filter(x -> x.getKey().equals(name) || x.getKey().startsWith(name + LocalizedString.LANG_SEPARATOR))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	protected void loadLocalizedString(Map<String, AttributeData> attributesMap, LocalizedString localizedString,
			Map<String, AttributeData> filteredAttrs) {
		filteredAttrs.forEach((key, value) -> {
			String[] keyParts = key.split(LocalizedString.LANG_SEPARATOR);
			String data = attributesMap.get(key).getValue().toString();
			if (keyParts.length == 1) {
				localizedString.setValue(data);
			} else {
				localizedString.setValue(data, Locale.forLanguageTag(keyParts[1].replace(LocalizedString.LANG_PREFIX + LocalizedString.LANG_JOINER, "")));
			}
		});
	}

}

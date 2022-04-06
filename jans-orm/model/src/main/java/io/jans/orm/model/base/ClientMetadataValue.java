/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.base;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Javier Rojas Blum
 * @version April 6, 2022
 */
public class ClientMetadataValue implements Serializable {

    private static final long serialVersionUID = -7651487701235873969L;

    private final Map<String, String> values;

    public static final String EMPTY_LANG_TAG = "";
    public static final String LANG_SEPARATOR = ";";
    public static final String LANG_CLAIM_SEPARATOR = "#";
    public static final String LANG_PREFIX = "lang";
    public static final String LANG_JOINER = "-";

    public ClientMetadataValue() {
        values = new HashMap<>();
    }

    public void setValue(String value) {
        values.put(EMPTY_LANG_TAG, value);
    }

    public void setValue(String value, Locale locale) {
        values.put(getLanguageTag(locale), value);
    }

    public String getValue() {
        return getValue(EMPTY_LANG_TAG);
    }

    public String getValue(String languageTag) {
        return values.getOrDefault(languageTag, null);
    }

    public int size() {
        return values.size();
    }

    public Set<String> getLanguageTags() {
        return values.keySet();
    }

    @Override
    public String toString() {
        return values.toString();
    }

    public String addLdapLanguageTag(String ldapAttributeName, String languageTag) {
        return ldapAttributeName + (StringUtils.isNotBlank(languageTag) ?
                LANG_SEPARATOR + LANG_PREFIX + LANG_JOINER + languageTag : EMPTY_LANG_TAG);
    }

    private String getLanguageTag(Locale locale) {
        List<String> keyParts = new ArrayList<>();
        keyParts.add(locale.getLanguage());
        keyParts.add(locale.getScript());
        keyParts.add(locale.getCountry());

        return keyParts.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(LANG_JOINER));
    }
}

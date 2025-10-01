/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Allowed authentication modes
 *
 * @author Yuriy Movchan Date: 08/08/2013
 */
public enum AuthenticationMode {

    BASIC("basic", "Basic"), OAUTH("oauth", "OAuth"), UMA("uma", "UMA");

    private final String value;
    private final String displayName;

    private static final Map<String, AuthenticationMode> MAP_BY_VALUES = new HashMap<String, AuthenticationMode>();

    static {
        for (AuthenticationMode enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    AuthenticationMode(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public static AuthenticationMode getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public String getDisplayName() {
        return displayName;
    }

    public AuthenticationMode resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}

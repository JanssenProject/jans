/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * Script languages
 *
 * @author Yuriy Movchan Date: 07/10/2013
 */
public enum ProgrammingLanguage implements AttributeEnum {

    PYTHON("python", "Jython"), JAVA("java", "Java");

    private final String value;
    private final String displayName;

    private static final Map<String, ProgrammingLanguage> MAP_BY_VALUES = new HashMap<>();

    static {
        for (ProgrammingLanguage enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    ProgrammingLanguage(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ProgrammingLanguage getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}

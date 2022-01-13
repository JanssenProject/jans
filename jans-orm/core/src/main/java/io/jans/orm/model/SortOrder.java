/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * @author Val Pecaoco
 */
public enum SortOrder implements AttributeEnum {

    ASCENDING("ascending"),
    DESCENDING("descending");

    private String value;

    private static Map<String, SortOrder> MAP_BY_VALUES = new HashMap<String, SortOrder>();

    static {
        for (SortOrder enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    SortOrder(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static SortOrder getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    @Override
    public SortOrder resolveByValue(String value) {
        return getByValue(value);
    }
}

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.types;

import java.util.HashMap;
import java.util.Map;

/**
 * Different layouts available to be used as logs.
 *
 */
public enum LoggingLayoutType {

    TEXT("TEXT"),
    JSON("JSON");

    private String value;

    private static Map<String, LoggingLayoutType> MAP_BY_VALUES = new HashMap<String, LoggingLayoutType>();

    static {
        for (LoggingLayoutType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    LoggingLayoutType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LoggingLayoutType getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    @Override
    public String toString() {
        return value;
    }

}

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
 * Authentication script type
 *
 * @author Yuriy Movchan Date: 05/06/2013
 */
public enum AuthenticationScriptUsageType implements AttributeEnum {

    INTERACTIVE("interactive", "Web"), SERVICE("service", "Native"), BOTH("both", "Both methods");

    private String value;
    private String displayName;

    private static Map<String, AuthenticationScriptUsageType> MAP_BY_VALUES = new HashMap<String, AuthenticationScriptUsageType>();

    static {
        for (AuthenticationScriptUsageType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    AuthenticationScriptUsageType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AuthenticationScriptUsageType getByValue(String value) {
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

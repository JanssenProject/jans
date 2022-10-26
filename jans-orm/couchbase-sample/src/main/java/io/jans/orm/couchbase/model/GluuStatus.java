/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.model;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * Status
 *
 * @author Yuriy Movchan Date: 10.07.2010
 */
public enum GluuStatus implements AttributeEnum {

    ACTIVE("active", "Active"), INACTIVE("inactive", "Inactive"), EXPIRED("expired", "Expired"), REGISTER("register", "Register");

    private String value;
    private String displayName;

    private static Map<String, GluuStatus> MAP_BY_VALUES = new HashMap<String, GluuStatus>();

    static {
        for (GluuStatus enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    GluuStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static GluuStatus getByValue(String value) {
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

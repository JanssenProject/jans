/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.token;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * Token entry status
 *
 * @author Yuriy Movchan
 * @version 1.0, 06/03/2024
 */
public enum TokenPoolStatus implements AttributeEnum {

    INUSE("inuse", "Inuse"), FREE("free", "Free");

    private String value;
    private String displayName;

    private static Map<String, TokenPoolStatus> MAP_BY_VALUES = new HashMap<String, TokenPoolStatus>();

    static {
        for (TokenPoolStatus enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    TokenPoolStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TokenPoolStatus getByValue(String value) {
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

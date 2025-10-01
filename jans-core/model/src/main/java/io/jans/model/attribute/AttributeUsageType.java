/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.attribute;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
 * Attribute Usage Type
 *
 * @author Yuriy Movchan Date: 02/12/2014
 */
public enum AttributeUsageType implements AttributeEnum {

    OPENID("openid", "OpenID");

    private String value;
    private String displayName;

    private static Map<String, AttributeUsageType> MAP_BY_VALUES = new HashMap<String, AttributeUsageType>();

    static {
        for (AttributeUsageType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    AttributeUsageType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AttributeUsageType getByValue(String value) {
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

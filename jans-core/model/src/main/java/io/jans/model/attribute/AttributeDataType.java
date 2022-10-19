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
 * Attribute Data Type
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 *
 * @version September 27, 2017
 */
public enum AttributeDataType implements AttributeEnum {

    STRING("string", "Text"), NUMERIC("numeric", "Numeric"), BOOLEAN("boolean", "Boolean"), BINARY("binary", "Binary"), CERTIFICATE("certificate",
            "Certificate"), DATE("generalizedTime", "Date"), JSON("json", "Json");

    private String value;
    private String displayName;

    private static Map<String, AttributeDataType> MAP_BY_VALUES = new HashMap<String, AttributeDataType>();

    static {
        for (AttributeDataType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    AttributeDataType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AttributeDataType getByValue(String value) {
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

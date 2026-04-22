/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.configapi.plugin.shibboleth.model;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;


public enum Status implements AttributeEnum {

    DRAFT("draft", "Draft"), VALID("valid", "Valid"), PENDING("pending", "Pending"), ACTIVE("active", "Active"), INACTIVE("inactive", "Inactive"), ERROR("error", "Error");

    private String value;
    private String displayName;

    private static Map<String, Status> MAP_BY_VALUES = new HashMap<String, Status>();

    static {
        for (Status enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    Status(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Status getByValue(String value) {
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

/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import java.util.HashMap;
import java.util.Map;

import org.gluu.persist.annotation.AttributeEnum;

/**
 * Boolean value
 *
 * @author Yuriy Movchan Date: 11.20.2010
 */
public enum GluuBoolean implements AttributeEnum {

    DISABLED(false, "disabled", "Disabled"), ENABLED(true, "enabled", "Enabled"), FALSE(false, "false", "False"), TRUE(true, "true", "True"),
    INACTIVE(false, "inactive", "Inactive"), ACTIVE(true, "active", "Active");

    private boolean booleanValue;
    private String value;
    private String displayName;

    private static Map<String, GluuBoolean> MAP_BY_VALUES = new HashMap<String, GluuBoolean>();

    static {
        for (GluuBoolean enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    GluuBoolean(boolean booleanValue, String value, String displayName) {
        this.booleanValue = booleanValue;
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public boolean isBooleanValue() {
        return booleanValue;
    }

    public static GluuBoolean getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}

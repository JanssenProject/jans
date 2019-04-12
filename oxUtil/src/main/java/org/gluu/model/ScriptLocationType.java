/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.persistence.annotation.AttributeEnum;

/**
 * Specify type of script location
 *
 * @author Yuriy Movchan Date: 10/07/2015
 */
public enum ScriptLocationType implements AttributeEnum {

    LDAP("ldap", "Ldap"), FILE("file", "File");

    private String value;
    private String displayName;

    private static Map<String, ScriptLocationType> MAP_BY_VALUES = new HashMap<String, ScriptLocationType>();

    static {
        for (ScriptLocationType enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    ScriptLocationType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ScriptLocationType getByValue(String value) {
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

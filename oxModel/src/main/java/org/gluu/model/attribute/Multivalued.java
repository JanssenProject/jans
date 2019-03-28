/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.gluu.model.attribute;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * SCIM Custom Attribute
 *
 * @author Reda Zerrad Date: 08.02.2012
 */
public enum Multivalued implements LdapEnum {

    TRUE("true", "True"), FALSE("false", "False");

    private String value;
    private String displayName;

    private static Map<String, Multivalued> MAP_BY_VALUES = new HashMap<String, Multivalued>();

    static {
        for (Multivalued enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    Multivalued(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Multivalued getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    public Enum<? extends LdapEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}

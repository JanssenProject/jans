/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.ldap.model;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Val Pecaoco
 */
public enum SortOrder implements LdapEnum {

    ASCENDING ("ascending"),
    DESCENDING ("descending");

    private String value;

    private static Map<String, SortOrder> mapByValues = new HashMap<String, SortOrder>();

    static {
        for (SortOrder enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private SortOrder(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static SortOrder getByValue(String value) {
        return mapByValues.get(value);
    }

    @Override
    public SortOrder resolveByValue(String value) {
        return getByValue(value);
    }
}

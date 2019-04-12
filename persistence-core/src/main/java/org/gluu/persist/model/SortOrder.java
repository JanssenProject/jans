/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.persistence.annotation.LdapEnum;

/**
 * @author Val Pecaoco
 */
public enum SortOrder implements LdapEnum {

    ASCENDING("ascending"),
    DESCENDING("descending");

    private String value;

    private static Map<String, SortOrder> MAP_BY_VALUES = new HashMap<String, SortOrder>();

    static {
        for (SortOrder enumType : values()) {
            MAP_BY_VALUES.put(enumType.getValue(), enumType);
        }
    }

    SortOrder(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static SortOrder getByValue(String value) {
        return MAP_BY_VALUES.get(value);
    }

    @Override
    public SortOrder resolveByValue(String value) {
        return getByValue(value);
    }
}

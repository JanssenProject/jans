/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/03/2013
 */

public enum UmaScopeType {

    PROTECTION("uma_protection");

    private static Map<String, UmaScopeType> lookup = new HashMap<>();

    static {
        for (UmaScopeType enumType : values()) {
            lookup.put(enumType.getValue(), enumType);
        }
    }

    private String value;

    private UmaScopeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UmaScopeType fromValue(String value) {
        return lookup.get(value);
    }
}

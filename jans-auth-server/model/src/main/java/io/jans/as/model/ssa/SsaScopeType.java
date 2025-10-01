/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.ssa;

import java.util.HashMap;
import java.util.Map;

public enum SsaScopeType {

    SSA_ADMIN("https://jans.io/auth/ssa.admin"),
    SSA_PORTAL("https://jans.io/auth/ssa.portal"),
    SSA_DEVELOPER("https://jans.io/auth/ssa.developer"),
    ;

    private static final Map<String, SsaScopeType> lookup = new HashMap<>();

    static {
        for (SsaScopeType enumType : values()) {
            lookup.put(enumType.getValue(), enumType);
        }
    }

    private final String value;

    SsaScopeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SsaScopeType fromValue(String value) {
        return lookup.get(value);
    }
}

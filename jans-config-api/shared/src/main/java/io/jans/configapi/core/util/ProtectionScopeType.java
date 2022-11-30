/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.StringUtils;

public enum ProtectionScopeType {

    SCOPE("scope"), GROUP("group"), SUPER("super");

    private final String scopeName;

    ProtectionScopeType(String scopeName) {
        this.scopeName = scopeName;
    }

    String getValue() {
        return toString().toLowerCase();
    }

    @JsonCreator
    public static ProtectionScopeType fromString(String scopeType) {
        if (StringUtils.isNotBlank(scopeType)) {
            for (ProtectionScopeType type : ProtectionScopeType.values()) {
                if (scopeType.equalsIgnoreCase(type.getValue())) {
                    return type;
                }
            }
        }
        return null;
    }

    @Override
    @JsonValue
    public String toString() {
        return scopeName;
    }

    public String getScopeName() {
        return scopeName;
    }
}

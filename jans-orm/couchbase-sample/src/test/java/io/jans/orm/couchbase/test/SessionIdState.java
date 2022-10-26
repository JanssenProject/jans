/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.test;

import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public enum SessionIdState implements AttributeEnum {

    UNAUTHENTICATED("unauthenticated"), AUTHENTICATED("authenticated");

    private final String value;

    private static Map<String, SessionIdState> mapByValues = new HashMap<String, SessionIdState>();

    static {
        for (SessionIdState enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private SessionIdState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SessionIdState getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }

}

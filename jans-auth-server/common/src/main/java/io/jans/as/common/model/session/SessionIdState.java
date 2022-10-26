/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.model.session;

import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 09/02/2015
 */

public enum SessionIdState implements AttributeEnum {

    UNAUTHENTICATED("unauthenticated"), AUTHENTICATED("authenticated");

    private final String value;

    private static final Map<String, SessionIdState> mapByValues = new HashMap<String, SessionIdState>();

    static {
        for (SessionIdState enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    SessionIdState(String value) {
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

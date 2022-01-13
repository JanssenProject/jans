/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.cloud.spanner.test;

import java.util.HashMap;
import java.util.Map;

import io.jans.orm.annotation.AttributeEnum;

/**
*
* @author Yuriy Movchan Date: 01/15/2020
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

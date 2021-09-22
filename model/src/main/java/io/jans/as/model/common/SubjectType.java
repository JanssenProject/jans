/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Rojas Blum Date: 05.11.2012
 */
public enum SubjectType implements AttributeEnum {

    PAIRWISE("pairwise"),
    PUBLIC("public");
    private static final Map<String, SubjectType> mapByValues = new HashMap<>();

    static {
        for (SubjectType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private final String paramName;

    SubjectType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link SubjectType} for an user id type parameter.
     *
     * @param param The parameter.
     * @return The corresponding user id type if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
    public static SubjectType fromString(String param) {
        if (param != null) {
            for (SubjectType uit : SubjectType.values()) {
                if (param.equals(uit.paramName)) {
                    return uit;
                }
            }
        }
        return null;
    }

    public static SubjectType getByValue(String value) {
        return mapByValues.get(value);
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the user id type parameter.
     */
    @Override
    @JsonValue
    public String toString() {
        return paramName;
    }

    public String getValue() {
        return paramName;
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }
}
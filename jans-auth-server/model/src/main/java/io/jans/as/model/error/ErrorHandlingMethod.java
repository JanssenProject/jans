/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.HasParamName;
import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * This class define error handling methods
 *
 * @author Javier Rojas Blum
 * @author Yuriy Movchan Date: 12/07/2018
 */
public enum ErrorHandlingMethod implements HasParamName, AttributeEnum {

    INTERNAL("internal"),

    REMOTE("remote");

    private final String value;

    private static final Map<String, ErrorHandlingMethod> mapByValues = new HashMap<>();

    static {
        for (ErrorHandlingMethod enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    ErrorHandlingMethod() {
        this.value = null;
    }

    ErrorHandlingMethod(String value) {
        this.value = value;
    }

    /**
     * Gets param name.
     *
     * @return param name
     */
    public String getParamName() {
        return value;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Returns the corresponding {@link GrantType} for a parameter grant_type of
     * the access token requests. For the extension grant type, the parameter
     * should be a valid URI.
     *
     * @param param The grant_type parameter.
     * @return The corresponding grant type if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
    public static ErrorHandlingMethod fromString(String param) {
        if (param != null) {
            for (ErrorHandlingMethod hm : ErrorHandlingMethod.values()) {
                if (hm.value.equalsIgnoreCase(param)) {
                    return hm;
                }
            }
        }

        return null;
    }

    public static String[] toStringArray(ErrorHandlingMethod[] grantTypes) {
        if (grantTypes == null) {
            return new String[0];
        }

        String[] resultGrantTypes = new String[grantTypes.length];
        for (int i = 0; i < grantTypes.length; i++) {
            resultGrantTypes[i] = grantTypes[i].getValue();
        }

        return resultGrantTypes;
    }

    public static ErrorHandlingMethod getByValue(String value) {
        return mapByValues.get(value);
    }

    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the grant_type parameter.
     *
     * @return The string representation of the object.
     */
    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}

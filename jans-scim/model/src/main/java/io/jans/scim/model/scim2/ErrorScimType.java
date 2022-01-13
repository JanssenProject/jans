/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

import java.util.HashMap;
import java.util.Map;

/**
 * Detail error types when a HTTP 400 response is served. See section 3.12 of RFC7644.
 * @author Val Pecaoco
 */
/*
 * Updated by jgomer on 2017-09-14.
 */
public enum ErrorScimType {

    INVALID_FILTER ("invalidFilter"),
    TOO_MANY ("tooMany"),
    UNIQUENESS ("uniqueness"),
    MUTABILITY ("mutability"),
    INVALID_SYNTAX ("invalidSyntax"),
    INVALID_PATH ("invalidPath"),
    NO_TARGET ("noTarget"),
    INVALID_VALUE ("invalidValue"),
    INVALID_VERSION ("invalidVers"),
    SENSITIVE ("sensitive");

    private String value;

    private static Map<String, ErrorScimType> mapByValues = new HashMap<>();

    static {
        for (ErrorScimType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    ErrorScimType(String value) {
        this.value = value;
    }

    /**
     * Returns the <code>scimType</code> as it should be included in a json error response, e.g. "mutability" or
     * "sensitive"
     * @return A string value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns an instance of <code>ErrorScimType</code> based on a string value corresponding to the <code>scimType</code>
     * property of a json error response. This is the reverse of {@link #getValue()} method.
     * @param value A string corresponding to <code>scimType</code>
     * @return A <code>ErrorScimType</code> instance or null if the value passed is unknown
     */
    public static ErrorScimType getByValue(String value) {
        return mapByValues.get(value);
    }

    @Override
    public String toString() {
        return getValue();
    }

}

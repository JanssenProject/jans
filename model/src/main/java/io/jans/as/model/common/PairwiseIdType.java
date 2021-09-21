/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Javier Rojas Blum
 * @version February 15, 2015
 */
public enum PairwiseIdType {

    ALGORITHMIC("algorithmic"),
    PERSISTENT("persistent");

    private final String value;

    private PairwiseIdType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PairwiseIdType fromString(String string) {
        for (PairwiseIdType v : values()) {
            if (v.getValue().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return null;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

}

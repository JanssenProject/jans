/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */

public enum ProgrammingLanguage {
    PYTHON("Python"),
    JAVA_SCRIPT("JavaScript");

    private final String value;

    ProgrammingLanguage(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProgrammingLanguage fromString(String string) {
        for (ProgrammingLanguage v : values()) {
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

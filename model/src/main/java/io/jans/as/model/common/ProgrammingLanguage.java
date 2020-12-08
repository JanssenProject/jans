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

    private final String m_value;

    private ProgrammingLanguage(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }

    @JsonCreator
    public static ProgrammingLanguage fromString(String p_string) {
        for (ProgrammingLanguage v : values()) {
            if (v.getValue().equalsIgnoreCase(p_string)) {
                return v;
            }
        }
        return null;
    }

    @Override
    @JsonValue
    public String toString() {
        return m_value;
    }

}

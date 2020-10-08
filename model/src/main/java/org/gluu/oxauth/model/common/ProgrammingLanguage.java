/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

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

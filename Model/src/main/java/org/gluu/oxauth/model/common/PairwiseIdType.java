package org.gluu.oxauth.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Javier Rojas Blum
 * @version February 15, 2015
 */
public enum PairwiseIdType {

    ALGORITHMIC("algorithmic"),
    PERSISTENT("persistent");

    private final String m_value;

    private PairwiseIdType(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }

    @JsonCreator
    public static PairwiseIdType fromString(String p_string) {
        for (PairwiseIdType v : values()) {
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

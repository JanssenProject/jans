package io.jans.configapi.core.model.adminui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum CedarlingLogType {
    off("off"),
    std_out("std_out");

    private final String value;
    private static final Map<String, CedarlingLogType> mapByValues = new HashMap<>();

    static {
        for (CedarlingLogType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    CedarlingLogType() {
        this.value = null;
    }

    CedarlingLogType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CedarlingLogType fromString(String param) {
        if (param != null) {
            for (CedarlingLogType gt : CedarlingLogType.values()) {
                if (param.equals(gt.value)) {
                    return gt;
                }
            }
        }
        return null;
    }

    /**
     * Lookup the enum constant that corresponds to the given string value.
     *
     * @param value the string value associated with a CedarlingLogType constant
     * @return the matching CedarlingLogType constant, or `null` if no match exists
     */
    public static CedarlingLogType getByValue(String value) {
        return mapByValues.get(value);
    }

    /**
     * Provides the enum's associated string value for JSON serialization.
     *
     * @return the underlying string value of this enum constant
     */
    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
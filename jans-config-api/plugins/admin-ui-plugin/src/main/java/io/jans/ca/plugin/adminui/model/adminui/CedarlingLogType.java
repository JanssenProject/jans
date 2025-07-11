package io.jans.ca.plugin.adminui.model.adminui;

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

    public static CedarlingLogType getByValue(String value) {
        return mapByValues.get(value);
    }

    @JsonValue
    public String toString() {
        return value;
    }
}

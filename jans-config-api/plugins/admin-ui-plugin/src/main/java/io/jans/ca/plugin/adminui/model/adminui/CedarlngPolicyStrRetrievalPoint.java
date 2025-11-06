package io.jans.ca.plugin.adminui.model.adminui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum CedarlngPolicyStrRetrievalPoint {
    DEFAULT("default"),
    REMOTE("remote");

    private final String value;
    private static final Map<String, CedarlngPolicyStrRetrievalPoint> mapByValues = new HashMap<>();

    static {
        for (CedarlngPolicyStrRetrievalPoint enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    CedarlngPolicyStrRetrievalPoint() {
        this.value = null;
    }

    CedarlngPolicyStrRetrievalPoint(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CedarlngPolicyStrRetrievalPoint fromString(String param) {
        if (param != null) {
            for (CedarlngPolicyStrRetrievalPoint gt : CedarlngPolicyStrRetrievalPoint.values()) {
                if (param.equals(gt.value)) {
                    return gt;
                }
            }
        }
        return null;
    }

    public static CedarlngPolicyStrRetrievalPoint getByValue(String value) {
        return mapByValues.get(value);
    }

    @JsonValue
    public String toString() {
        return value;
    }
}

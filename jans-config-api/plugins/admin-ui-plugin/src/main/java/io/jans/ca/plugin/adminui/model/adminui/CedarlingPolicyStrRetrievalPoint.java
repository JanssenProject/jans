package io.jans.ca.plugin.adminui.model.adminui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum CedarlingPolicyStrRetrievalPoint {
    DEFAULT("default"),
    REMOTE("remote");

    private final String value;
    private static final Map<String, CedarlingPolicyStrRetrievalPoint> mapByValues = new HashMap<>();

    static {
        for (CedarlingPolicyStrRetrievalPoint enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    CedarlingPolicyStrRetrievalPoint(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CedarlingPolicyStrRetrievalPoint fromString(String param) {
        return getByValue(param);
    }

    public static CedarlingPolicyStrRetrievalPoint getByValue(String value) {
        return mapByValues.get(value);
    }

    @JsonValue
    public String toString() {
        return value;
    }
}

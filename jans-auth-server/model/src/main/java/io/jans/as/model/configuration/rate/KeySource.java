package io.jans.as.model.configuration.rate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Where to extract the key from.
 * <p>
 * Defensive behavior:
 * - Unknown values deserialize to {@link #UNKNOWN} instead of failing.
 * - Serialization uses the json value (lower-case).
 */
public enum KeySource {
    BODY("body"),
    HEADER("header"),
    QUERY("query"),
    UNKNOWN("unknown");

    private final String jsonValue;

    KeySource(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonCreator
    public static KeySource fromJson(String value) {
        if (value == null) return null; // preserve null if field absent
        String v = value.trim();
        if (v.isEmpty()) return null;

        for (KeySource s : values()) {
            if (s.jsonValue.equalsIgnoreCase(v)) {
                return s;
            }
        }
        // Defensive: don't hard-fail on new/typo values
        return UNKNOWN;
    }

    @JsonValue
    public String toJson() {
        return jsonValue;
    }
}

package io.jans.model.tokenstatus;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Z
 */
public enum TokenStatus {
    CREATED(0),
    REVOKED(1);

    private final int value;

    private static Map<Integer, TokenStatus> mapByValues = new HashMap<>();

    static {
        for (TokenStatus enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    TokenStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TokenStatus fromValue(int value) {
        return mapByValues.get(value);
    }
}

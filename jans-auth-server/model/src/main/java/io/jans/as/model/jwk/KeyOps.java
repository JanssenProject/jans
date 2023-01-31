package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author Yuriy Z
 */
public enum KeyOps {
    CONNECT("connect"),
    SSA("ssa"),

    ALL("all");

    private final String value;

    KeyOps(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static KeyOps fromString(String valueString) {
        if (valueString != null) {
            for (KeyOps v : values()) {
                if (valueString.equalsIgnoreCase(v.name())) {
                    return v;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "KeyOps{" +
                "value='" + value + '\'' +
                "} " + super.toString();
    }
}



package org.gluu.oxd.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.StringUtils;

public enum ExpiredObjectType {
    STATE("state"),
    NONCE("nonce"),
    REQUEST_OBJECT("request_object"),
    JWKS("jwks");

    private final String value;

    ExpiredObjectType(String p_value) {
        value = p_value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ExpiredObjectType fromValue(String v) {
        if (StringUtils.isNotBlank(v)) {
            for (ExpiredObjectType t : values()) {
                if (t.getValue().equalsIgnoreCase(v)) {
                    return t;
                }
            }
        }
        return null;
    }
}

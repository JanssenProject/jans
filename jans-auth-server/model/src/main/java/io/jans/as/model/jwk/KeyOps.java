package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

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

    public static List<KeyOps> fromJSONArray(JSONArray jsonArray) {
        List<KeyOps> result = new ArrayList<>();
        if (jsonArray == null) {
            return result;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            final KeyOps v = fromString(jsonArray.optString(i));
            if (v != null) {
                result.add(v);
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "KeyOps{" +
                "value='" + value + '\'' +
                "} " + super.toString();
    }
}



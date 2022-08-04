package io.jans.as.model.common;

import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Z
 */
public enum CreatorType implements AttributeEnum {
    NONE("none"),
    CLIENT("client"),
    USER("user"),
    AUTO("auto");

    private static final Map<String, CreatorType> mapByValues = new HashMap<>();

    static {
        for (CreatorType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private final String value;

    CreatorType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static CreatorType getByValue(String value) {
        return mapByValues.get(value);
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

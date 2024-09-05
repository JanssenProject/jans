package io.jans.as.model.common;

import io.jans.orm.annotation.AttributeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Z
 */
public enum XFrameOptions implements HasParamName, AttributeEnum {

    /**
     * The page can only be displayed if all ancestor frames are same origin to the page itself.
     */
    SAMEORIGIN,

    /**
     * The page cannot be displayed in a frame, regardless of the site attempting to do so.
     */
    DENY;

    private static final Map<String, XFrameOptions> mapByValues = new HashMap<>();

    static {
        for (XFrameOptions enumType : values()) {
            mapByValues.put(enumType.getParamName(), enumType);
        }
    }

    @Override
    public String getParamName() {
        return name();
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return mapByValues.get(value);
    }
}

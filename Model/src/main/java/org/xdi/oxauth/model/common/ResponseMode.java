package org.xdi.oxauth.model.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Rojas Blum Date: 03.21.2014
 */
public enum ResponseMode implements HasParamName {

    QUERY("query"),
    FRAGMENT("fragment");

    private final String value;

    private static Map<String, ResponseMode> mapByValues = new HashMap<String, ResponseMode>();

    static {
        for (ResponseMode enumType : values()) {
            mapByValues.put(enumType.getParamName(), enumType);
        }
    }

    private ResponseMode(String value) {
        this.value = value;
    }

    public static ResponseMode getByValue(String value) {
        return mapByValues.get(value);
    }

    @Override
    public String getParamName() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
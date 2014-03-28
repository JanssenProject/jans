package org.xdi.oxauth.model.uma.persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */

public enum UmaScopeType {
    INTERNAL("internal"),
    EXTERNAL("external"),
    EXTERNAL_AUTO("external_auto");

    private static Map<String, UmaScopeType> lookup = new HashMap<String, UmaScopeType>();

    static {
        for (UmaScopeType enumType : values()) {
            lookup.put(enumType.getValue(), enumType);
        }
    }

    private final String m_value;

    private UmaScopeType(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }

    public static UmaScopeType getByValue(String value) {
        return lookup.get(value);
    }
}

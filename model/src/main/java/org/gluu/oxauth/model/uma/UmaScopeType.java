/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/03/2013
 */

public enum UmaScopeType {

    PROTECTION("uma_protection");

    private static Map<String, UmaScopeType> lookup = new HashMap<String, UmaScopeType>();

    static {
        for (UmaScopeType enumType : values()) {
            lookup.put(enumType.getValue(), enumType);
        }
    }

    private String m_value;

    private UmaScopeType(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }

    public static UmaScopeType fromValue(String p_value) {
        return lookup.get(p_value);
    }
}

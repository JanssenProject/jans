package org.xdi.oxauth.model.common;

import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/01/2013
 */

public enum Mode {
    IN_MEMORY("memory"), LDAP("ldap");

    public static final Mode DEFAULT = IN_MEMORY;

    private final String m_value;

    private Mode(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }

    public static Mode fromValue(String p_value) {
        if (StringUtils.isNotBlank(p_value)) {
            for (Mode m : values()) {
                if (m.getValue().equals(p_value)) {
                    return m;
                }
            }
        }
        return null;
    }

    public static Mode fromValueWithDefault(String p_value) {
        final Mode result = fromValue(p_value);
        return result != null ? result : DEFAULT;
    }
}

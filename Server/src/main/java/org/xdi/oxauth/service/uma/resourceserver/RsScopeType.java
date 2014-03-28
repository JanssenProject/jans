package org.xdi.oxauth.service.uma.resourceserver;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/07/2013
 */

public enum RsScopeType {
    GENERATE_ID("generate_id");

    private final String m_value;

    private RsScopeType(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }
}

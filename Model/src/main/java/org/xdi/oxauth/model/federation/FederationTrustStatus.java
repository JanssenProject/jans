package org.xdi.oxauth.model.federation;

import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/11/2012
 */

public enum FederationTrustStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    INACTIVE_BY_CHECKER("inactive_by_checker");

    private final String m_value;

    private FederationTrustStatus(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }

    public static FederationTrustStatus fromValue(String p_trustStatus) {
        if (StringUtils.isNotBlank(p_trustStatus)) {
            for (FederationTrustStatus v : values()) {
                if (v.getValue().equalsIgnoreCase(p_trustStatus)) {
                    return v;
                }
            }
        }
        return null;
    }
}

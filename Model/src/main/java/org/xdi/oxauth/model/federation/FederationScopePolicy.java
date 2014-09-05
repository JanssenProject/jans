/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.federation;

import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/11/2012
 */

public enum FederationScopePolicy {
    JOIN("JOIN");

    public static final FederationScopePolicy DEFAULT = JOIN;

    private final String m_value;

    private FederationScopePolicy(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }

    public static FederationScopePolicy fromString(String p_value) {
        if (StringUtils.isNotBlank(p_value)) {
            for (FederationScopePolicy p : values()) {
                if (p_value.equalsIgnoreCase(p.getValue())) {
                    return p;
                }
            }
        }
        return null;
    }

    public static FederationScopePolicy fromStringWithDefault(String p_value) {
        final FederationScopePolicy result = fromString(p_value);
        return result != null ? result : DEFAULT;
    }

}

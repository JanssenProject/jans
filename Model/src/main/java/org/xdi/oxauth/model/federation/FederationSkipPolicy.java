/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.federation;

import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/11/2012
 */

public enum FederationSkipPolicy {
    OR("OR"), AND("AND");

    public static final FederationSkipPolicy DEFAULT = OR;

    private final String m_value;

    private FederationSkipPolicy(String p_value) {
        m_value = p_value;
    }

    public String getValue() {
        return m_value;
    }

    public static FederationSkipPolicy fromString(String p_value) {
        if (StringUtils.isNotBlank(p_value)) {
            for (FederationSkipPolicy p : values()) {
                if (p_value.equalsIgnoreCase(p.getValue())) {
                    return p;
                }
            }
        }
        return null;
    }

    public static FederationSkipPolicy fromStringWithDefault(String p_value) {
        final FederationSkipPolicy result = fromString(p_value);
        return result != null ? result : DEFAULT;
    }
}

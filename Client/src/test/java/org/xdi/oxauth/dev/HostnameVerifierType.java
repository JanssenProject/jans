/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.dev;

import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/09/2012
 */

public enum HostnameVerifierType {
    DEFAULT("default"),
    ALLOW_ALL("allow_all");

    private final String m_value;

    HostnameVerifierType(String p_value) {
        m_value = p_value;
    }

    public static HostnameVerifierType fromString(String p_value) {
        if (StringUtils.isNotBlank(p_value)) {
            for (HostnameVerifierType v : values()) {
                if (v.m_value.equals(p_value)) {
                    return v;
                }
            }
        }
        return DEFAULT;
    }
}

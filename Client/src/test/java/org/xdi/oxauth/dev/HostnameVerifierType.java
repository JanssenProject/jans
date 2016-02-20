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

    private final String value;

    HostnameVerifierType(String value) {
        this.value = value;
    }

    public static HostnameVerifierType fromString(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (HostnameVerifierType v : values()) {
                if (v.value.equals(value)) {
                    return v;
                }
            }
        }
        return DEFAULT;
    }
}

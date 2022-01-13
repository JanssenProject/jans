/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.ldap;

import org.apache.commons.lang.StringUtils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 08/01/2013
 */
public enum TokenType {
    ID_TOKEN("id_token"),
    ACCESS_TOKEN("access_token"),
    LONG_LIVED_ACCESS_TOKEN("access_token"),
    REFRESH_TOKEN("refresh_token"),
    AUTHORIZATION_CODE("authorization_code");

    private final String value;

    TokenType(String name) {
        value = name;
    }

    public static TokenType fromValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (TokenType t : values()) {
                if (t.getValue().endsWith(value)) {
                    return t;
                }
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }
}

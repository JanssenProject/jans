/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ldap;

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

    public String getValue() {
        return value;
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
}

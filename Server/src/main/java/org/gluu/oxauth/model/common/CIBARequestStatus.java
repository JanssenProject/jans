/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import org.apache.commons.lang.StringUtils;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
public enum CIBARequestStatus {
    AUTHORIZATION_PENDING("pending"),
    AUTHORIZATION_GRANTED("granted"),
    AUTHORIZATION_DENIED("denied"),
    AUTHORIZATION_EXPIRED("expired"),
    AUTHORIZATION_IN_PROCESS("in_process");

    private final String value;

    CIBARequestStatus(String name) {
        value = name;
    }

    public String getValue() {
        return value;
    }

    public static CIBARequestStatus fromValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (CIBARequestStatus t : values()) {
                if (t.getValue().endsWith(value)) {
                    return t;
                }
            }
        }
        return null;
    }
}

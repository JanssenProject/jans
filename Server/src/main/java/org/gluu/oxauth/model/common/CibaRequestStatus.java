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
public enum CibaRequestStatus {
    PENDING("pending"),
    GRANTED("granted"),
    DENIED("denied"),
    EXPIRED("expired"),
    IN_PROCESS("in_process");

    private final String value;

    CibaRequestStatus(String name) {
        value = name;
    }

    public String getValue() {
        return value;
    }

    public static CibaRequestStatus fromValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (CibaRequestStatus t : values()) {
                if (t.getValue().endsWith(value)) {
                    return t;
                }
            }
        }
        return null;
    }
}

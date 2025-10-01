/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import org.apache.commons.lang3.StringUtils;

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

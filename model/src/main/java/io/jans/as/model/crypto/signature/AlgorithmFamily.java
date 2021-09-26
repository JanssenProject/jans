/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public enum AlgorithmFamily {
    NONE("none"),
    HMAC("HMAC"),
    RSA("RSA"),
    EC("EC"),
    ED("ED"),
    AES("AES"),
    PASSW("PASSW"),
    DIR("DIR");

    private final String value;

    AlgorithmFamily(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }

    @JsonCreator
    public static AlgorithmFamily fromString(String param) {
        if (param != null) {
            for (AlgorithmFamily gt : AlgorithmFamily.values()) {
                if (param.equals(gt.value)) {
                    return gt;
                }
            }
        }

        return null;
    }

}
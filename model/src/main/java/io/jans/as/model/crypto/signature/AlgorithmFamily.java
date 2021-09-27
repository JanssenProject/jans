/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.jans.as.model.jwk.KeyType;

/**
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public enum AlgorithmFamily {
    NONE("none", null),
    HMAC("HMAC", KeyType.OCT),
    RSA("RSA", KeyType.RSA),
    EC("EC", KeyType.EC),
    ED("ED", KeyType.OKP),
    AES("AES", KeyType.OCT),
    PASSW("PASSW", KeyType.OCT),
    DIR("DIR", null);

    private final String value;
    private final KeyType keyType;

    AlgorithmFamily(final String value, final KeyType keyType) {
        this.value = value;
        this.keyType = keyType;
    }

    public String getValue() {
        return value;
    }

    public KeyType getKeyType() {
        return keyType;
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
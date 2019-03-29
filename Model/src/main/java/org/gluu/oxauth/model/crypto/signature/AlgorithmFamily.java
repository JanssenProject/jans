/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.crypto.signature;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public enum AlgorithmFamily {
    HMAC("HMAC"),
    RSA("RSA"),
    EC("EC");

    private final String value;

    AlgorithmFamily(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

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
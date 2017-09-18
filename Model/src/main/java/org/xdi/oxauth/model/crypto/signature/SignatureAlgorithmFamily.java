/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

/**
 * @author Javier Rojas Blum
 * @version August 28, 2017
 */
public enum SignatureAlgorithmFamily {
    HMAC("HMAC"),
    RSA("RSA"),
    EC("EC");

    private final String value;

    SignatureAlgorithmFamily(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static SignatureAlgorithmFamily fromString(String param) {
        if (param != null) {
            for (SignatureAlgorithmFamily gt : SignatureAlgorithmFamily.values()) {
                if (param.equals(gt.value)) {
                    return gt;
                }
            }
        }

        return null;
    }
}

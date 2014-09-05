/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

/**
 * Identifies the cryptographic algorithm family used with the key.
 *
 * @author Javier Rojas Blum Date: 1.10.2013
 */
public enum KeyType {

    /**
     * The Elliptic Curve Digital Signature Algorithm (ECDSA) is defined by FIPS 186‑3.
     */
    EC("EC"),

    /**
     * The RSA algorithm is defined by RFC 3447.
     */
    RSA("RSA");

    private final String paramName;

    private KeyType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link KeyType} for a parameter use of the JWK endpoint.
     *
     * @param param The use parameter.
     * @return The corresponding algorithm if found, otherwise <code>null</code>.
     */
    public static KeyType fromString(String param) {
        if (param != null) {
            for (KeyType keyType : KeyType.values()) {
                if (param.equals(keyType.paramName)) {
                    return keyType;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     *
     * @return The string representation of the object.
     */
    @Override
    public String toString() {
        return paramName;
    }
}
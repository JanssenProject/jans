/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jwk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Identifies the cryptographic algorithm family used with the key.
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
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
     * @return The corresponding algorithm family if found, otherwise <code>null</code>.
     */
    @JsonCreator
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
    @JsonValue
    public String toString() {
        return paramName;
    }
}
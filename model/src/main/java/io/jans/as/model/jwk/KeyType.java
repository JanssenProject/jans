/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Identifies the cryptographic algorithm family used with the key.
 *
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021 
 */
public enum KeyType {

    /**
     * The Elliptic Curve Digital Signature Algorithm (ECDSA) is defined by FIPS 186‑3.
     */
    EC("EC"),

    /**
     * The RSA algorithm is defined by RFC 3447.
     */
    RSA("RSA"),

    /**
     * Octet Key Pair.
     *
     * A new key type (kty) value "OKP" (Octet Key Pair) is defined for public key
     * algorithms that use octet strings as private and public keys. Defined by RFC
     * 8037 (CFRG Elliptic Curve Diffie-Hellman (ECDH) and Signatures in JSON Object
     * Signing and Encryption (JOSE)).
     * 
     * The Edwards Curve Digital Signature Algorithm (EDDSA) is defined by RFC 8032.
     */
    OKP("OKP"),

    /**
     * Octet sequence (used to represent symmetric keys), according to RFC 7518
     * (JSON Web Algorithms (JWA))
     */
    OCT("oct");

    private final String paramName;

    KeyType(String paramName) {
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
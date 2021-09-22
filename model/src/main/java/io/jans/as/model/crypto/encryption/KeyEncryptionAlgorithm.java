/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.jans.as.model.jwk.Algorithm;

/**
 * @author Javier Rojas Blum Date: 12.03.2012
 */
public enum KeyEncryptionAlgorithm {

    RSA1_5("RSA1_5", "RSA", "RSA/ECB/PKCS1Padding"),
    RSA_OAEP("RSA-OAEP", "RSA", "RSA/ECB/OAEPWithSHA1AndMGF1Padding"),
    A128KW("A128KW"),
    A256KW("A256KW");
    //DIR("dir"), // Not supported
    //ECDH_ES("ECDH-ES"), // Not supported
    //ECDH_ES_PLUS_A128KW("ECDH-ES+A128KW"), // Not supported
    //ECDH_ES_A256KW("ECDH-ES+A256KW"); // Not supported

    private final String name;
    private final String family;
    private final String algorithm;
    private final Algorithm alg;

    KeyEncryptionAlgorithm(String name) {
        this.name = name;
        this.family = null;
        this.algorithm = null;
        this.alg = Algorithm.fromString(name);
    }

    KeyEncryptionAlgorithm(String name, String family, String algorithm) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.alg = Algorithm.fromString(name);
    }

    public Algorithm getAlg() {
        return alg;
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return family;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    @JsonCreator
    public static KeyEncryptionAlgorithm fromName(String name) {
        if (name != null) {
            for (KeyEncryptionAlgorithm a : KeyEncryptionAlgorithm.values()) {
                if (name.equals(a.name)) {
                    return a;
                }
            }
        }
        return null;
    }

    @Override
    @JsonValue
    public String toString() {
        return name;
    }
}
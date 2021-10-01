/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.EllipticEdvardsCurve;
import io.jans.as.model.jwk.Algorithm;

/**
 * @author Javier Rojas Blum Date: 12.03.2012
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public enum KeyEncryptionAlgorithm {

    RSA1_5("RSA1_5", AlgorithmFamily.RSA, "RSA/ECB/PKCS1Padding"),
    RSA_OAEP("RSA-OAEP", AlgorithmFamily.RSA, "RSA/ECB/OAEPWithSHA1AndMGF1Padding"),
    RSA_OAEP_256("RSA-OAEP-256", AlgorithmFamily.RSA, "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"),

    ECDH_ES("ECDH-ES", AlgorithmFamily.EC, EllipticEdvardsCurve.P_256),
    ECDH_ES_PLUS_A128KW("ECDH-ES+A128KW", AlgorithmFamily.EC, EllipticEdvardsCurve.P_256),
    ECDH_ES_PLUS_A192KW("ECDH-ES+A192KW", AlgorithmFamily.EC, EllipticEdvardsCurve.P_256),
    ECDH_ES_PLUS_A256KW("ECDH-ES+A256KW", AlgorithmFamily.EC, EllipticEdvardsCurve.P_256),

    A128KW("A128KW", AlgorithmFamily.AES),
    A192KW("A192KW", AlgorithmFamily.AES),
    A256KW("A256KW", AlgorithmFamily.AES),

    A128GCMKW("A128GCMKW", AlgorithmFamily.AES),
    A192GCMKW("A192GCMKW", AlgorithmFamily.AES),
    A256GCMKW("A256GCMKW", AlgorithmFamily.AES),

    PBES2_HS256_PLUS_A128KW("PBES2-HS256+A128KW", AlgorithmFamily.PASSW),
    PBES2_HS384_PLUS_A192KW("PBES2-HS384+A192KW", AlgorithmFamily.PASSW),
    PBES2_HS512_PLUS_A256KW("PBES2-HS512+A256KW", AlgorithmFamily.PASSW),

    DIR("dir", AlgorithmFamily.DIR);

    private final String name;
    private final String algorithm;
    private final Algorithm alg;
    private final EllipticEdvardsCurve curve;
    private final AlgorithmFamily family;

    private KeyEncryptionAlgorithm(String name, AlgorithmFamily family) {
        this.name = name;
        this.family = family;
        this.algorithm = null;
        this.curve = null;
        this.alg = Algorithm.fromString(name);
    }

    private KeyEncryptionAlgorithm(String name, AlgorithmFamily family, String algorithm) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.curve = null;
        this.alg = Algorithm.fromString(name);
    }

    private KeyEncryptionAlgorithm(String name, AlgorithmFamily family, EllipticEdvardsCurve curve) {
        this.name = name;
        this.family = family;
        this.algorithm = null;
        this.curve = curve;
        this.alg = Algorithm.fromString(name);
    }

    public Algorithm getAlg() {
        return alg;
    }

    public String getName() {
        return name;
    }

    public AlgorithmFamily getFamily() {
        return family;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public EllipticEdvardsCurve getCurve() {
        return curve;
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
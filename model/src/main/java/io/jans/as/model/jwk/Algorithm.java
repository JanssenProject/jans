/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Identifies the cryptographic algorithm used with the key.
 *
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public enum Algorithm {

    // Signature
    RS256("RS256", "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-256", Use.SIGNATURE, AlgorithmFamily.RSA),
    RS384("RS384", "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-384", Use.SIGNATURE, AlgorithmFamily.RSA),
    RS512("RS512", "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-512", Use.SIGNATURE, AlgorithmFamily.RSA),

    ES256("ES256", "Signature Key: ECDSA using P-256 (secp256r1) and SHA-256", Use.SIGNATURE, AlgorithmFamily.EC),
    ES256K("ES256K", "Signature Key: ECDSA using secp256k1 and SHA-256", Use.SIGNATURE, AlgorithmFamily.EC),
    ES384("ES384", "Signature Key: ECDSA using P-384 (secp384r1) and SHA-384", Use.SIGNATURE, AlgorithmFamily.EC),
    ES512("ES512", "Signature Key: ECDSA using P-521 (secp521r1) and SHA-512", Use.SIGNATURE, AlgorithmFamily.EC),

    PS256("PS256", "Signature Key: RSASSA-PSS using SHA-256 and MGF1 with SHA-256", Use.SIGNATURE, AlgorithmFamily.RSA),
    PS384("PS384", "Signature Key: RSASSA-PSS using SHA-384 and MGF1 with SHA-384", Use.SIGNATURE, AlgorithmFamily.RSA),
    PS512("PS512", "Signature Key: RSASSA-PSS using SHA-512 and MGF1 with SHA-512", Use.SIGNATURE, AlgorithmFamily.RSA),

    ED25519("Ed25519", "Signature Key: EDDSA using Ed25519 with SHA-512", Use.SIGNATURE, AlgorithmFamily.ED),
    ED448("Ed448", "Signature Key: EDDSA using Ed448 with SHA-3/SHAKE256", Use.SIGNATURE, AlgorithmFamily.ED),

    // Encryption
    RSA1_5("RSA1_5", "Encryption Key: RSAES-PKCS1-v1_5",
            Use.ENCRYPTION, AlgorithmFamily.RSA),
    RSA_OAEP("RSA-OAEP", "Encryption Key: RSAES OAEP using default parameters",
            Use.ENCRYPTION, AlgorithmFamily.RSA),
    RSA_OAEP_256("RSA-OAEP-256", "Encryption Key: RSAES OAEP using SHA-256 and MGF1 with SHA-256 ",
            Use.ENCRYPTION, AlgorithmFamily.RSA),

    ECDH_ES("ECDH-ES", "Encryption Key: Elliptic Curve Diffie-Hellman Ephemeral Static key agreement using Concat KDF",
            Use.ENCRYPTION, AlgorithmFamily.EC),
    ECDH_ES_PLUS_A128KW("Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A128KW", "ECDH-ES+A128KW",
            Use.ENCRYPTION, AlgorithmFamily.EC),
    ECDH_ES_PLUS_A192KW("Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A192KW", "ECDH-ES+A192KW",
            Use.ENCRYPTION, AlgorithmFamily.EC),
    ECDH_ES_PLUS_A256KW("Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A256KW", "ECDH-ES+A256KW",
            Use.ENCRYPTION, AlgorithmFamily.EC),

    A128KW("A128KW", "Encryption Key: AES Key Wrap with default initial value using 128-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES),
    A192KW("A192KW", "Encryption Key: AES Key Wrap with default initial value using 192-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES),
    A256KW("A256KW", "Encryption Key:  AES Key Wrap with default initial value using 192-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES),

    A128GCMKW("A128GCMKW", "Encryption Key: Key wrapping with AES GCM using 128-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES),
    A192GCMKW("A192GCMKW", "Encryption Key: Key wrapping with AES GCM using 192-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES),
    A256GCMKW("A256GCMKW", "Encryption Key: Key wrapping with AES GCM using 256-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES),

    PBES2_HS256_PLUS_A128KW("PBES2-HS256+A128KW", "Encryption Key: PBES2 with HMAC SHA-256 and A128KW wrapping",
            Use.ENCRYPTION, AlgorithmFamily.PASSW),
    PBES2_HS384_PLUS_A192KW("PBES2-HS384+A192KW", "Encryption Key: PBES2 with HMAC SHA-384 and A192KW wrapping",
            Use.ENCRYPTION, AlgorithmFamily.PASSW),
    PBES2_HS512_PLUS_A256KW("PBES2-HS512+A256KW", "Encryption Key: PBES2 with HMAC SHA-512 and A256KW wrapping",
            Use.ENCRYPTION, AlgorithmFamily.PASSW),

    DIR("dir", "Encryption Key: Direct use of a shared symmetric key as the CEK",
            Use.ENCRYPTION, AlgorithmFamily.DIR);

    private final String paramName;
    private final String paramDescription;
    private final Use use;
    private final AlgorithmFamily family;

    Algorithm(String paramName, String paramDescription, Use use, AlgorithmFamily family) {
        this.paramName = paramName;
        this.paramDescription = paramDescription;
        this.use = use;
        this.family = family;
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamDescription() {
        return paramDescription;
    }

    public Use getUse() {
        return use;
    }

    public AlgorithmFamily getFamily() {
        return family;
    }

    /**
     * Returns the corresponding {@link Algorithm} for a parameter.
     *
     * @param param The use parameter.
     * @return The corresponding algorithm if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static Algorithm fromString(String param) {
        if (param != null) {
            for (Algorithm algorithm : Algorithm.values()) {
                if (param.equals(algorithm.paramName)) {
                    return algorithm;
                }
            }
        }
        return null;
    }

    public static List<Algorithm> fromString(String[] params, Use use) {
        List<Algorithm> algorithms = new ArrayList<>();

        for (String param : params) {
            Algorithm algorithm = Algorithm.fromString(param);
            if (algorithm != null && algorithm.use == use) {
                algorithms.add(algorithm);
            } else if (StringUtils.equals("RSA_OAEP", param)) {
                algorithms.add(RSA_OAEP);
            }
        }

        return algorithms;
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
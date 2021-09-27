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
    RS256("RS256", Use.SIGNATURE, AlgorithmFamily.RSA),
    RS384("RS384", Use.SIGNATURE, AlgorithmFamily.RSA),
    RS512("RS512", Use.SIGNATURE, AlgorithmFamily.RSA),

    ES256("ES256", Use.SIGNATURE, AlgorithmFamily.EC),
    ES256K("ES256K", Use.SIGNATURE, AlgorithmFamily.EC),
    ES384("ES384", Use.SIGNATURE, AlgorithmFamily.EC),
    ES512("ES512", Use.SIGNATURE, AlgorithmFamily.EC),

    PS256("PS256", Use.SIGNATURE, AlgorithmFamily.RSA),
    PS384("PS384", Use.SIGNATURE, AlgorithmFamily.RSA),
    PS512("PS512", Use.SIGNATURE, AlgorithmFamily.RSA),

    ED25519("Ed25519", Use.SIGNATURE, AlgorithmFamily.ED),
    ED448("Ed448", Use.SIGNATURE, AlgorithmFamily.ED),

    // Encryption
    RSA1_5("RSA1_5", Use.ENCRYPTION, AlgorithmFamily.RSA),
    RSA_OAEP("RSA-OAEP", Use.ENCRYPTION, AlgorithmFamily.RSA),
    RSA_OAEP_256("RSA-OAEP-256", Use.ENCRYPTION, AlgorithmFamily.RSA),
    
    ECDH_ES("ECDH-ES", Use.ENCRYPTION, AlgorithmFamily.EC),
    ECDH_ES_PLUS_A128KW("ECDH-ES+A128KW", Use.ENCRYPTION, AlgorithmFamily.EC),
    ECDH_ES_PLUS_A192KW("ECDH-ES+A192KW", Use.ENCRYPTION, AlgorithmFamily.EC),
    ECDH_ES_PLUS_A256KW("ECDH-ES+A256KW", Use.ENCRYPTION, AlgorithmFamily.EC),

    A128KW("A128KW", Use.ENCRYPTION, AlgorithmFamily.AES),
    A192KW("A192KW", Use.ENCRYPTION, AlgorithmFamily.AES),
    A256KW("A256KW", Use.ENCRYPTION, AlgorithmFamily.AES),

    A128GCMKW("A128GCMKW", Use.ENCRYPTION, AlgorithmFamily.AES),
    A192GCMKW("A192GCMKW", Use.ENCRYPTION, AlgorithmFamily.AES),
    A256GCMKW("A256GCMKW", Use.ENCRYPTION, AlgorithmFamily.AES),

    PBES2_HS256_PLUS_A128KW("PBES2-HS256+A128KW", Use.ENCRYPTION, AlgorithmFamily.PASSW),
    PBES2_HS384_PLUS_A192KW("PBES2-HS384+A192KW", Use.ENCRYPTION, AlgorithmFamily.PASSW),
    PBES2_HS512_PLUS_A256KW("PBES2-HS512+A256KW", Use.ENCRYPTION, AlgorithmFamily.PASSW);

    private final String paramName;
    private final Use use;
    private final AlgorithmFamily family;

    Algorithm(String paramName, Use use, AlgorithmFamily family) {
        this.paramName = paramName;
        this.use = use;
        this.family = family;
    }

    public String getParamName() {
        return paramName;
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
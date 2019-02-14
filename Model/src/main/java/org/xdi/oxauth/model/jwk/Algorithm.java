/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;
import org.xdi.oxauth.model.crypto.signature.AlgorithmFamily;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Identifies the cryptographic algorithm used with the key.
 *
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public enum Algorithm {

    // Signature
    RS256("RS256", Use.SIGNATURE, AlgorithmFamily.RSA),
    RS384("RS384", Use.SIGNATURE, AlgorithmFamily.RSA),
    RS512("RS512", Use.SIGNATURE, AlgorithmFamily.RSA),
    ES256("ES256", Use.SIGNATURE, AlgorithmFamily.EC),
    ES384("ES384", Use.SIGNATURE, AlgorithmFamily.EC),
    ES512("ES512", Use.SIGNATURE, AlgorithmFamily.EC),
    PS256("PS256", Use.SIGNATURE, AlgorithmFamily.RSA),
    PS384("PS384", Use.SIGNATURE, AlgorithmFamily.RSA),
    PS512("PS512", Use.SIGNATURE, AlgorithmFamily.RSA),

    // Encryption
    RSA1_5("RSA1_5", Use.ENCRYPTION, AlgorithmFamily.RSA),
    RSA_OAEP("RSA-OAEP", Use.ENCRYPTION, AlgorithmFamily.RSA);

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
        List<Algorithm> algorithms = new ArrayList<Algorithm>();

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
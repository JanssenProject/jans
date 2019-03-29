/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;
import org.xdi.oxauth.model.jwt.JwtType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public enum SignatureAlgorithm {

    NONE("none"),
    HS256("HS256", AlgorithmFamily.HMAC, "HMACSHA256"),
    HS384("HS384", AlgorithmFamily.HMAC, "HMACSHA384"),
    HS512("HS512", AlgorithmFamily.HMAC, "HMACSHA512"),
    RS256("RS256", AlgorithmFamily.RSA, "SHA256WITHRSA"),
    RS384("RS384", AlgorithmFamily.RSA, "SHA384WITHRSA"),
    RS512("RS512", AlgorithmFamily.RSA, "SHA512WITHRSA"),
    ES256("ES256", AlgorithmFamily.EC, "SHA256WITHECDSA", ECEllipticCurve.P_256),
    ES384("ES384", AlgorithmFamily.EC, "SHA384WITHECDSA", ECEllipticCurve.P_384),
    ES512("ES512", AlgorithmFamily.EC, "SHA512WITHECDSA", ECEllipticCurve.P_521),
    PS256("PS256", AlgorithmFamily.RSA, "SHA256withRSAandMGF1"),
    PS384("PS384", AlgorithmFamily.RSA, "SHA384withRSAandMGF1"),
    PS512("PS512", AlgorithmFamily.RSA, "SHA512withRSAandMGF1");

    private final String name;
    private final AlgorithmFamily family;
    private final String algorithm;
    private final ECEllipticCurve curve;
    private final JwtType jwtType;

    SignatureAlgorithm(String name, AlgorithmFamily family, String algorithm, ECEllipticCurve curve) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.curve = curve;
        this.jwtType = JwtType.JWT;
    }

    SignatureAlgorithm(String name, AlgorithmFamily family, String algorithm) {
        this(name, family, algorithm, null);
    }

    SignatureAlgorithm(String name) {
        this(name, null, null, null);
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

    public ECEllipticCurve getCurve() {
        return curve;
    }

    public JwtType getJwtType() {
        return jwtType;
    }

    public static List<SignatureAlgorithm> fromString(String[] params) {
        List<SignatureAlgorithm> signatureAlgorithms = new ArrayList<SignatureAlgorithm>();

        for (String param : params) {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(param);
            if (signatureAlgorithm != null) {
                signatureAlgorithms.add(signatureAlgorithm);
            }
        }

        return signatureAlgorithms;
    }

    /**
     * Returns the corresponding {@link SignatureAlgorithm} for a parameter alg of the JWK endpoint.
     *
     * @param param The alg parameter.
     * @return The corresponding alg if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static SignatureAlgorithm fromString(String param) {
        if (param != null) {
            for (SignatureAlgorithm sa : SignatureAlgorithm.values()) {
                if (param.equals(sa.name)) {
                    return sa;
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
        return name;
    }
}
/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.nimbusds.jose.JWSAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwt.JwtType;

import java.util.ArrayList;
import java.util.List;

/**
 * Signature Algorithms.
 *
 * JWS digital signature and MAC "alg" (algorithm) values
 * (RFC 7518, A.1.  Digital Signature/MAC Algorithm Identifier
 * Cross-Reference).
 *
 * CFRG Elliptic Curve Diffie-Hellman (ECDH) and Signatures
 * in JSON Object Signing and Encryption (JOSE) signature
 * algorithm "Ed25519". 
 *
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version October 26, 2021
 */
public enum SignatureAlgorithm {

    NONE("none", AlgorithmFamily.NONE, null, null),

    HS256(SignatureAlgorithm.DEF_HS256, AlgorithmFamily.HMAC, SignatureAlgorithm.DEF_HMACSHA256, JWSAlgorithm.HS256),
    HS384(SignatureAlgorithm.DEF_HS384, AlgorithmFamily.HMAC, SignatureAlgorithm.DEF_HMACSHA384, JWSAlgorithm.HS384),
    HS512(SignatureAlgorithm.DEF_HS512, AlgorithmFamily.HMAC, SignatureAlgorithm.DEF_HMACSHA512, JWSAlgorithm.HS512),

    RS256(SignatureAlgorithm.DEF_RS256, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA256WITHRSA, JWSAlgorithm.RS256),
    RS384(SignatureAlgorithm.DEF_RS384, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA384WITHRSA, JWSAlgorithm.RS384),
    RS512(SignatureAlgorithm.DEF_RS512, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA512WITHRSA, JWSAlgorithm.RS512),

    ES256(SignatureAlgorithm.DEF_ES256, AlgorithmFamily.EC, SignatureAlgorithm.DEF_SHA256WITHECDSA, EllipticEdvardsCurve.P_256, JWSAlgorithm.ES256),
    ES256K(SignatureAlgorithm.DEF_ES256K, AlgorithmFamily.EC, SignatureAlgorithm.DEF_SHA256WITHECDSA, EllipticEdvardsCurve.P_256K, JWSAlgorithm.ES256K),
    ES384(SignatureAlgorithm.DEF_ES384, AlgorithmFamily.EC, SignatureAlgorithm.DEF_SHA384WITHECDSA, EllipticEdvardsCurve.P_384, JWSAlgorithm.ES384),
    ES512(SignatureAlgorithm.DEF_ES512, AlgorithmFamily.EC, SignatureAlgorithm.DEF_SHA512WITHECDSA, EllipticEdvardsCurve.P_521, JWSAlgorithm.ES512),

    PS256(SignatureAlgorithm.DEF_PS256, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA256WITHRSAANDMGF1, JWSAlgorithm.PS256),
    PS384(SignatureAlgorithm.DEF_PS384, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA384WITHRSAANDMGF1, JWSAlgorithm.PS384),
    PS512(SignatureAlgorithm.DEF_PS512, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA512WITHRSAANDMGF1, JWSAlgorithm.PS512),

    EDDSA(SignatureAlgorithm.DEF_EDDDSA, AlgorithmFamily.ED, SignatureAlgorithm.DEF_ED25519, EllipticEdvardsCurve.ED_25519, JWSAlgorithm.EdDSA);

    public static final String DEF_HS256 = "HS256";
    public static final String DEF_HS384 = "HS384";
    public static final String DEF_HS512 = "HS512";

    public static final String DEF_RS256 = "RS256";
    public static final String DEF_RS384 = "RS384";
    public static final String DEF_RS512 = "RS512";

    public static final String DEF_ES256 = "ES256";
    public static final String DEF_ES256K = "ES256K";
    public static final String DEF_ES384 = "ES384";
    public static final String DEF_ES512 = "ES512";

    public static final String DEF_PS256 = "PS256";
    public static final String DEF_PS384 = "PS384";
    public static final String DEF_PS512 = "PS512";

    public static final String DEF_ED25519 = "Ed25519";
    public static final String DEF_EDDDSA = "EdDSA";

    public static final String DEF_HMACSHA256 = "HMACSHA256";
    public static final String DEF_HMACSHA384 = "HMACSHA384";
    public static final String DEF_HMACSHA512 = "HMACSHA512";

    public static final String DEF_SHA256WITHRSA = "SHA256WITHRSA";
    public static final String DEF_SHA384WITHRSA = "SHA384WITHRSA";
    public static final String DEF_SHA512WITHRSA = "SHA512WITHRSA";

    public static final String DEF_SHA256WITHECDSA = "SHA256WITHECDSA";
    public static final String DEF_SHA384WITHECDSA = "SHA384WITHECDSA";
    public static final String DEF_SHA512WITHECDSA = "SHA512WITHECDSA";

    public static final String DEF_SHA256WITHRSAANDMGF1 = "SHA256withRSAandMGF1";
    public static final String DEF_SHA384WITHRSAANDMGF1 = "SHA384withRSAandMGF1";
    public static final String DEF_SHA512WITHRSAANDMGF1 = "SHA512withRSAandMGF1";

    private final String name;
    private final AlgorithmFamily family;
    private final String algorithm;
    private final EllipticEdvardsCurve curve;
    private final JwtType jwtType;
    private final JWSAlgorithm jwsAlgorithm;
    private final Algorithm alg;

    SignatureAlgorithm(String name, AlgorithmFamily family, String algorithm, EllipticEdvardsCurve curve, JWSAlgorithm jwsAlgorithm) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.curve = curve;
        this.jwtType = JwtType.JWT;
        this.jwsAlgorithm = jwsAlgorithm;
        this.alg = Algorithm.fromString(name);
    }

    SignatureAlgorithm(String name, AlgorithmFamily family, String algorithm, JWSAlgorithm jwsAlgorithm) {
        this(name, family, algorithm, null, jwsAlgorithm);
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

    public JwtType getJwtType() {
        return jwtType;
    }

    public static List<SignatureAlgorithm> fromString(String[] params) {
        List<SignatureAlgorithm> signatureAlgorithms = new ArrayList<>();

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

    /**
     * Returns this.jwsAlgorithm (JSON Web Signature (JWS) algorithm name)
     *
     * @return this.jwsAlgorithm (JSON Web Signature (JWS) algorithm name)
     */
    public JWSAlgorithm getJwsAlgorithm() {
        return jwsAlgorithm;
    }
}

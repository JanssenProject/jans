/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.as.model.common.HasParamName;
import io.jans.as.model.jwt.JwtType;
import io.jans.orm.annotation.AttributeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Asymmetric Signature Algorithms.
 * Subset of signature algorithms, which use asymmetric algorithms
 * (RSA, ECDSA, EDDSA).
 * 
 * JWS digital signature and MAC "alg" (algorithm) values
 * (RFC 7518, A.1.  Digital Signature/MAC Algorithm Identifier
 * Cross-Reference).
 *
 * CFRG Elliptic Curve Diffie-Hellman (ECDH) and Signatures
 * in JSON Object Signing and Encryption (JOSE) signature
 * algorithms "Ed25519" and "Ed448".
 * 
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public enum AsymmetricSignatureAlgorithm implements HasParamName, AttributeEnum {

    RS256(SignatureAlgorithm.DEF_RS256, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA256WITHRSA),
    RS384(SignatureAlgorithm.DEF_RS384, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA384WITHRSA),
    RS512(SignatureAlgorithm.DEF_RS512, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA512WITHRSA),
    
    ES256(SignatureAlgorithm.DEF_ES256, AlgorithmFamily.EC, SignatureAlgorithm.DEF_SHA256WITHECDSA, EllipticEdvardsCurve.P_256),
    ES256K(SignatureAlgorithm.DEF_ES256K, AlgorithmFamily.EC, SignatureAlgorithm.DEF_SHA256WITHECDSA, EllipticEdvardsCurve.P_256K),    
    ES384(SignatureAlgorithm.DEF_ES384, AlgorithmFamily.EC, SignatureAlgorithm.DEF_SHA384WITHECDSA, EllipticEdvardsCurve.P_384),
    ES512(SignatureAlgorithm.DEF_ES512, AlgorithmFamily.EC, SignatureAlgorithm.DEF_SHA512WITHECDSA, EllipticEdvardsCurve.P_521),

    PS256(SignatureAlgorithm.DEF_PS256, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA256WITHRSAANDMGF1),
    PS384(SignatureAlgorithm.DEF_PS384, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA384WITHRSAANDMGF1),
    PS512(SignatureAlgorithm.DEF_PS512, AlgorithmFamily.RSA, SignatureAlgorithm.DEF_SHA512WITHRSAANDMGF1),

    ED25519(SignatureAlgorithm.DEF_ED25519, AlgorithmFamily.ED, SignatureAlgorithm.DEF_ED25519, EllipticEdvardsCurve.ED_25519),
    ED448(SignatureAlgorithm.DEF_ED448,     AlgorithmFamily.ED, SignatureAlgorithm.DEF_ED448,   EllipticEdvardsCurve.ED_448),
    EDDSA(SignatureAlgorithm.DEF_EDDDSA,    AlgorithmFamily.ED, SignatureAlgorithm.DEF_EDDDSA,  EllipticEdvardsCurve.ED_25519);

    private final String name;
    private final AlgorithmFamily family;
    private final String algorithm;
    private final EllipticEdvardsCurve curve;
    private final JwtType jwtType;

    private static final Map<String, AsymmetricSignatureAlgorithm> mapByValues = new HashMap<>();

    static {
        for (AsymmetricSignatureAlgorithm enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    AsymmetricSignatureAlgorithm(String name, AlgorithmFamily family, String algorithm, EllipticEdvardsCurve curve) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.curve = curve;
        this.jwtType = JwtType.JWT;
    }

    AsymmetricSignatureAlgorithm(String name, AlgorithmFamily family, String algorithm) {
        this(name, family, algorithm, null);
    }

    @Override
    public String getParamName() {
        return name;
    }

    @Override
    public String getValue() {
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

    public static List<AsymmetricSignatureAlgorithm> fromString(String[] params) {
        List<AsymmetricSignatureAlgorithm> asymmetricSignatureAlgorithms = new ArrayList<>();

        for (String param : params) {
            AsymmetricSignatureAlgorithm asymmetricSignatureAlgorithm = AsymmetricSignatureAlgorithm.fromString(param);
            if (asymmetricSignatureAlgorithm != null) {
                asymmetricSignatureAlgorithms.add(asymmetricSignatureAlgorithm);
            }
        }

        return asymmetricSignatureAlgorithms;
    }

    /**
     * Returns the corresponding {@link AsymmetricSignatureAlgorithm} for a parameter alg of the JWK endpoint.
     *
     * @param param The alg parameter.
     * @return The corresponding alg if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static AsymmetricSignatureAlgorithm fromString(String param) {
        if (param != null) {
            for (AsymmetricSignatureAlgorithm sa : AsymmetricSignatureAlgorithm.values()) {
                if (param.equals(sa.name)) {
                    return sa;
                }
            }
        }
        return null;
    }

    public static AsymmetricSignatureAlgorithm getByValue(String value) {
        return mapByValues.get(value);
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String value) {
        return getByValue(value);
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
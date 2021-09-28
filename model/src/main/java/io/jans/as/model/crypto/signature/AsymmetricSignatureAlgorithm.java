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
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public enum  AsymmetricSignatureAlgorithm implements HasParamName, AttributeEnum {

    RS256("RS256", AlgorithmFamily.RSA, "SHA256WITHRSA"),
    RS384("RS384", AlgorithmFamily.RSA, "SHA384WITHRSA"),
    RS512("RS512", AlgorithmFamily.RSA, "SHA512WITHRSA"),
    ES256("ES256", AlgorithmFamily.EC, "SHA256WITHECDSA", EllipticEdvardsCurve.P_256),
    ES384("ES384", AlgorithmFamily.EC, "SHA384WITHECDSA", EllipticEdvardsCurve.P_384),
    ES512("ES512", AlgorithmFamily.EC, "SHA512WITHECDSA", EllipticEdvardsCurve.P_521),
    PS256("PS256", AlgorithmFamily.RSA, "SHA256withRSAandMGF1"),
    PS384("PS384", AlgorithmFamily.RSA, "SHA384withRSAandMGF1"),
    PS512("PS512", AlgorithmFamily.RSA, "SHA512withRSAandMGF1");

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
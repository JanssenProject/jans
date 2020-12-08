/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.model;

/**
 * @author Javier Rojas Blum
 * @version May 4, 2016
 */
public enum SignatureAlgorithm {

    NONE("none"),
    HS256("HS256", SignatureAlgorithmFamily.HMAC, "HMACSHA256"),
    HS384("HS384", SignatureAlgorithmFamily.HMAC, "HMACSHA384"),
    HS512("HS512", SignatureAlgorithmFamily.HMAC, "HMACSHA512"),
    RS256("RS256", SignatureAlgorithmFamily.RSA, "SHA256WITHRSA"),
    RS384("RS384", SignatureAlgorithmFamily.RSA, "SHA384WITHRSA"),
    RS512("RS512", SignatureAlgorithmFamily.RSA, "SHA512WITHRSA"),
    ES256("ES256", SignatureAlgorithmFamily.EC, "SHA256WITHECDSA", ECEllipticCurve.P_256),
    ES384("ES384", SignatureAlgorithmFamily.EC, "SHA384WITHECDSA", ECEllipticCurve.P_384),
    ES512("ES512", SignatureAlgorithmFamily.EC, "SHA512WITHECDSA", ECEllipticCurve.P_521);

    private final String name;
    private final String family;
    private final String algorithm;
    private final ECEllipticCurve curve;

    private SignatureAlgorithm(String name, String family, String algorithm, ECEllipticCurve curve) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.curve = curve;
    }

    private SignatureAlgorithm(String name, String family, String algorithm) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.curve = null;
    }

    private SignatureAlgorithm(String name) {
        this.name = name;
        this.family = null;
        this.algorithm = null;
        this.curve = null;
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

    public ECEllipticCurve getCurve() {
        return curve;
    }

    public static SignatureAlgorithm fromName(String name) {
        if (name != null) {
            for (SignatureAlgorithm sa : SignatureAlgorithm.values()) {
                if (name.equals(sa.name)) {
                    return sa;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}

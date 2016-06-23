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
 * @version June 15, 2016
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
    private final JwtType jwtType;

    private SignatureAlgorithm(String name, String family, String algorithm, ECEllipticCurve curve) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.curve = curve;
        this.jwtType = JwtType.JWT;
    }

    private SignatureAlgorithm(String name, String family, String algorithm) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.curve = null;
        this.jwtType = JwtType.JWT;
    }

    private SignatureAlgorithm(String name) {
        this.name = name;
        this.family = null;
        this.algorithm = null;
        this.curve = null;
        this.jwtType = JwtType.JWT;
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
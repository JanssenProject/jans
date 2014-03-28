package org.xdi.oxauth.model.crypto.signature;

/**
 * @author Javier Rojas Blum Date: 05.09.2012
 */
public enum SignatureAlgorithm {

    NONE("none"),
    HS256("HS256", "HMAC", "HMACSHA256"),
    HS384("HS384", "HMAC", "HMACSHA384"),
    HS512("HS512", "HMAC", "HMACSHA512"),
    RS256("RS256", "RSA", "SHA256WITHRSA"),
    RS384("RS384", "RSA", "SHA384WITHRSA"),
    RS512("RS512", "RSA", "SHA512WITHRSA"),
    ES256("ES256", "EC", "SHA256WITHECDSA", "P-256"),
    ES384("ES384", "EC", "SHA384WITHECDSA", "P-384"),
    ES512("ES512", "EC", "SHA512WITHECDSA", "P-521");

    private final String name;
    private final String family;
    private final String algorithm;
    private final String curve;

    private SignatureAlgorithm(String name, String family, String algorithm, String curve) {
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

    public String getCurve() {
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
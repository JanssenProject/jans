/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.jose.jwk.JWKException;
import io.jans.as.model.crypto.signature.EllipticEdvardsCurve;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.StringUtils;
import io.jans.as.model.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class JSONWebKey {

    private static final String KTY_PARAM_APPEND = "\"kty\":";
    private static final String CRV_PARAM_APPEND = "\"crv\":";
    private static final String N_PARAM_APPEND = "\"n\":";
    private static final String E_PARAM_APPEND = "\"e\":";
    private static final String X_PARAM_APPEND = "\"x\":";
    private static final String Y_PARAM_APPEND = "\"y\":";

    private String name;
    private String descr;
    private String kid;
    private KeyType kty;
    private Use use;
    private Algorithm alg;
    private Long exp;
    private EllipticEdvardsCurve crv;
    private List<String> x5c;

    /**
     * Modulus
     */
    private String n;

    /**
     * Exponent
     */
    private String e;

    private String x;
    private String y;

    /**
     * Returns the Key Name.
     *
     * @return the Key Name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the Key Name.
     *
     * @param name the Key Name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the Key Description.
     *
     * @return the Key Description
     */
    public String getDescr() {
        return this.descr;
    }

    /**
     * Sets the Key Description.
     *
     * @param description the Key Description
     */
    public void setDescr(final String description) {
        this.descr = description;
    }

    /**
     * Returns the Key ID. The Key ID member can be used to match a specific key. This can be used, for instance,
     * to choose among a set of keys within the JWK during key rollover.
     *
     * @return The Key ID.
     */
    public String getKid() {
        return kid;
    }

    /**
     * Sets the Key ID.
     *
     * @param kid The Key ID.
     */
    public void setKid(String kid) {
        this.kid = kid;
    }

    public KeyType getKty() {
        return kty;
    }

    public void setKty(KeyType kty) {
        this.kty = kty;
    }

    /**
     * Returns the intended use of the key: signature or encryption.
     *
     * @return The intended use of the key.
     */
    public Use getUse() {
        return use;
    }

    /**
     * Sets the intended use of the key: signature or encryption.
     *
     * @param use The intended use of the key.
     */
    public void setUse(Use use) {
        this.use = use;
    }

    public Algorithm getAlg() {
        return alg;
    }

    public void setAlg(Algorithm alg) {
        this.alg = alg;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    /**
     * Returns the curve member that identifies the cryptographic curve used with the key.
     *
     * @return The curve member that identifies the cryptographic curve used with the key.
     */
    public EllipticEdvardsCurve getCrv() {
        return crv;
    }

    /**
     * Sets the curve member that identifies the cryptographic curve used with the key.
     *
     * @param crv The curve member that identifies the cryptographic curve used with the key.
     */
    public void setCrv(EllipticEdvardsCurve crv) {
        this.crv = crv;
    }

    public List<String> getX5c() {
        return x5c;
    }

    public void setX5c(List<String> x5c) {
        this.x5c = x5c;
    }

    /**
     * Returns the modulus value for the RSA public key. It is represented as the base64url encoding of the value's
     * representation.
     *
     * @return The modulus value for the RSA public key.
     */
    public String getN() {
        return n;
    }

    /**
     * Sets the modulus value for the RSA public key.
     *
     * @param n The modulus value for the RSA public key.
     */
    public void setN(String n) {
        this.n = n;
    }

    /**
     * Returns the exponent value for the RSA public key.
     *
     * @return The exponent value for the RSA public key.
     */
    public String getE() {
        return e;
    }

    /**
     * Sets the exponent value for the RSA public key.
     *
     * @param e The exponent value for the RSA public key.
     */
    public void setE(String e) {
        this.e = e;
    }

    /**
     * Returns the x member that contains the x coordinate for the elliptic curve point. It is represented as the
     * base64url encoding of the coordinate's big endian representation.
     *
     * @return The x member that contains the x coordinate for the elliptic curve point.
     */
    public String getX() {
        return x;
    }

    /**
     * Sets the x member that contains the x coordinate for the elliptic curve point.
     *
     * @param x The x member that contains the x coordinate for the elliptic curve point.
     */
    public void setX(String x) {
        this.x = x;
    }

    /**
     * Returns the y member that contains the x coordinate for the elliptic curve point. It is represented as the
     * base64url encoding of the coordinate's big endian representation.
     *
     * @return The y member that contains the x coordinate for the elliptic curve point.
     */
    public String getY() {
        return y;
    }

    /**
     * Sets the y member that contains the y coordinate for the elliptic curve point.
     *
     * @param y The y member that contains the y coordinate for the elliptic curve point.
     */
    public void setY(String y) {
        this.y = y;
    }

    /**
     * Steps:
     * <p>
     * 1. Construct a JSON object containing only the required members of a JWK representing the key and with no
     * whitespace or line breaks before or after any syntactic elements and with the required members ordered
     * lexicographically by the Unicode points of the member names. (This JSON object is itself a legal JWK
     * representation of the key.
     * <p>
     * 2. Hash the octets of the UTF-8 representation of this JSON object with a cryptographic hash function SHA-256.
     * <p>
     * 3. Encode the JKW SHA-256 Thumbprint with base64url encoding.
     *
     * @return The thumbprint of a JSON Web Key (JWK)
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc7638">JSON Web Key (JWK) Thumbprint</a>
     */
    @JsonIgnore
    public String getJwkThumbprint() throws NoSuchAlgorithmException, NoSuchProviderException, JWKException {
        String result;

        if (kty == null) throw new JWKException("The kty param is required");

        if (kty == KeyType.RSA) {
            result = contructJwkRSA();
        } else if (kty == KeyType.EC) {
            result = contructJwkEC();
        } else if (kty == KeyType.OKP) {
            result = contructJwkOKP();
        } else throw new JWKException("Thumbprint not supported for the kty");

        return result;
    }

    private String contructJwkRSA() throws NoSuchAlgorithmException, NoSuchProviderException, JWKException {
        if (e == null) throw new JWKException("The e param is required");
        if (n == null) throw new JWKException("The n param is required");

        String jwkStr = new StringBuilder()
                .append("{")
                .append(E_PARAM_APPEND).append("\"").append(e).append("\",")
                .append(KTY_PARAM_APPEND).append("\"").append(kty).append("\",")
                .append(N_PARAM_APPEND).append("\"").append(n).append("\"")
                .append("}")
                .toString();

        byte[] hash = JwtUtil.getMessageDigestSHA256(jwkStr);
        return Base64Util.base64urlencode(hash);
    }

    private String contructJwkEC() throws NoSuchAlgorithmException, NoSuchProviderException, JWKException {
        if (crv == null) throw new JWKException("The crv is required");
        if (x == null) throw new JWKException("The x is required");
        if (y == null) throw new JWKException("The y is required");

        String jwkStr = new StringBuilder()
                .append("{")
                .append(CRV_PARAM_APPEND).append("\"").append(crv).append("\",")
                .append(KTY_PARAM_APPEND).append("\"").append(kty).append("\",")
                .append(X_PARAM_APPEND).append("\"").append(x).append("\",")
                .append(Y_PARAM_APPEND).append("\"").append(y).append("\"")
                .append("}")
                .toString();

        byte[] hash = JwtUtil.getMessageDigestSHA256(jwkStr);
        return Base64Util.base64urlencode(hash);
    }

    private String contructJwkOKP() throws NoSuchAlgorithmException, NoSuchProviderException, JWKException {
        if (crv == null) throw new JWKException("The crv is required");
        if (x == null) throw new JWKException("The x is required");

        String jwkStr = new StringBuilder()
                .append("{")
                .append(CRV_PARAM_APPEND).append("\"").append(crv).append("\",")
                .append(KTY_PARAM_APPEND).append("\"").append(kty).append("\",")
                .append(X_PARAM_APPEND).append("\"").append(y).append("\"")
                .append("}")
                .toString();

        byte[] hash = JwtUtil.getMessageDigestSHA256(jwkStr);
        return Base64Util.base64urlencode(hash);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObj = new JSONObject();

        if (name != null) {
            jsonObj.put(JWKParameter.NAME, name);
        }
        if (descr != null) {
            jsonObj.put(JWKParameter.DESCRIPTION, descr);
        }
        jsonObj.put(JWKParameter.KEY_ID, kid);
        jsonObj.put(JWKParameter.KEY_TYPE, kty);
        if (use != null) {
            jsonObj.put(JWKParameter.KEY_USE, use.getParamName());
        }
        jsonObj.put(JWKParameter.ALGORITHM, alg);
        jsonObj.put(JWKParameter.EXPIRATION_TIME, exp);
        if (crv != null) {
            jsonObj.put(JWKParameter.CURVE, crv.getName());
        }
        if (!Util.isNullOrEmpty(n)) {
            jsonObj.put(JWKParameter.MODULUS, n);
        }
        if (!Util.isNullOrEmpty(e)) {
            jsonObj.put(JWKParameter.EXPONENT, e);
        }
        if (!Util.isNullOrEmpty(x)) {
            jsonObj.put(JWKParameter.X, x);
        }
        if (!Util.isNullOrEmpty(y)) {
            jsonObj.put(JWKParameter.Y, y);
        }
        if (x5c != null && !x5c.isEmpty()) {
            jsonObj.put(JWKParameter.CERTIFICATE_CHAIN, StringUtils.toJSONArray(x5c));
        }

        return jsonObj;
    }

    public static JSONWebKey fromJSONObject(JSONObject jwkJSONObject) throws JSONException {
        JSONWebKey jwk = new JSONWebKey();

        jwk.setName(jwkJSONObject.optString(JWKParameter.NAME));
        jwk.setDescr(jwkJSONObject.optString(JWKParameter.DESCRIPTION));
        jwk.setKid(jwkJSONObject.optString(JWKParameter.KEY_ID));
        jwk.setKty(KeyType.fromString(jwkJSONObject.optString(JWKParameter.KEY_TYPE)));
        jwk.setUse(Use.fromString(jwkJSONObject.optString(JWKParameter.KEY_USE)));
        jwk.setAlg(Algorithm.fromString(jwkJSONObject.optString(JWKParameter.ALGORITHM)));
        if (jwkJSONObject.has(JWKParameter.EXPIRATION_TIME)) {
            jwk.setExp(jwkJSONObject.optLong(JWKParameter.EXPIRATION_TIME));
        }
        jwk.setCrv(EllipticEdvardsCurve.fromString(jwkJSONObject.optString(JWKParameter.CURVE)));
        if (jwkJSONObject.has(JWKParameter.MODULUS)) {
            jwk.setN(jwkJSONObject.optString(JWKParameter.MODULUS));
        }
        if (jwkJSONObject.has(JWKParameter.EXPONENT)) {
            jwk.setE(jwkJSONObject.optString(JWKParameter.EXPONENT));
        }
        if (jwkJSONObject.has(JWKParameter.X)) {
            jwk.setX(jwkJSONObject.optString(JWKParameter.X));
        }
        if (jwkJSONObject.has(JWKParameter.Y)) {
            jwk.setY(jwkJSONObject.optString(JWKParameter.Y));
        }
        if (jwkJSONObject.has(JWKParameter.CERTIFICATE_CHAIN)) {
            jwk.setX5c(StringUtils.toList(jwkJSONObject.optJSONArray(JWKParameter.CERTIFICATE_CHAIN)));
        }

        return jwk;
    }
}

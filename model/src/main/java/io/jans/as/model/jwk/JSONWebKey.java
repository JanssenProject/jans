/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwk;

import io.jans.as.model.crypto.signature.EllipticEdvardsCurve;
import io.jans.as.model.util.StringUtils;
import io.jans.as.model.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class JSONWebKey implements Comparable<JSONWebKey> {

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

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObj = new JSONObject();

        jsonObj.put(JWKParameter.KEY_ID, kid);
        jsonObj.put(JWKParameter.KEY_TYPE, kty);
        jsonObj.put(JWKParameter.KEY_USE, use != null ? use.getParamName() : "");
        jsonObj.put(JWKParameter.ALGORITHM, alg);
        jsonObj.put(JWKParameter.EXPIRATION_TIME, exp);
        jsonObj.put(JWKParameter.CURVE, crv != null ? crv.getName() : "");
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

    @Override
    public int compareTo(JSONWebKey o) {
        if (this.getExp() == null || o.getExp() == null) {
            return 0;
        }

        return getExp().compareTo(o.getExp());
    }

    public static JSONWebKey fromJSONObject(JSONObject jwkJSONObject) throws JSONException {
        JSONWebKey jwk = new JSONWebKey();

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
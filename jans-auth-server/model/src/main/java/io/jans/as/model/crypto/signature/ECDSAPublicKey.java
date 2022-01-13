/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import io.jans.as.model.crypto.PublicKey;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;

import static io.jans.as.model.jwk.JWKParameter.EXPONENT;
import static io.jans.as.model.jwk.JWKParameter.MODULUS;
import static io.jans.as.model.jwk.JWKParameter.X;
import static io.jans.as.model.jwk.JWKParameter.Y;

/**
 * The Public Key for the Elliptic Curve Digital Signature Algorithm (ECDSA)
 *
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class ECDSAPublicKey extends PublicKey {

    private SignatureAlgorithm signatureAlgorithm;
    private BigInteger x;
    private BigInteger y;

    public ECDSAPublicKey(SignatureAlgorithm signatureAlgorithm, BigInteger x, BigInteger y) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.x = x;
        this.y = y;
    }

    public ECDSAPublicKey(SignatureAlgorithm signatureAlgorithm, String x, String y) {
        this(signatureAlgorithm,
                new BigInteger(1, Base64Util.base64urldecode(x)),
                new BigInteger(1, Base64Util.base64urldecode(y)));
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    @Override
    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public BigInteger getX() {
        return x;
    }

    public void setX(BigInteger x) {
        this.x = x;
    }

    public BigInteger getY() {
        return y;
    }

    public void setY(BigInteger y) {
        this.y = y;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(MODULUS, JSONObject.NULL);
        jsonObject.put(EXPONENT, JSONObject.NULL);
        jsonObject.put(X, Base64Util.base64urlencodeUnsignedBigInt(x));
        jsonObject.put(Y, Base64Util.base64urlencodeUnsignedBigInt(y));

        return jsonObject;
    }

    @Override
    public String toString() {
        try {
            return toJSONObject().toString(4);
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }
}
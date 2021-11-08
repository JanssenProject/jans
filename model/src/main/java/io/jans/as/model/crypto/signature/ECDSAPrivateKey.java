/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import static io.jans.as.model.jwk.JWKParameter.D;
import static io.jans.as.model.jwk.JWKParameter.EXPONENT;
import static io.jans.as.model.jwk.JWKParameter.MODULUS;

import java.math.BigInteger;

import org.json.JSONException;
import org.json.JSONObject;

import io.jans.as.model.crypto.PrivateKey;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.StringUtils;

/**
 * The Private Key for the Elliptic Curve Digital Signature Algorithm (ECDSA)
 *
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class ECDSAPrivateKey extends PrivateKey {

    private BigInteger d;

    public ECDSAPrivateKey(SignatureAlgorithm signatureAlgorithm, BigInteger d) {
        setSignatureAlgorithm(signatureAlgorithm);
        this.d = d;
    }

    public ECDSAPrivateKey(String d) {
        this.d = new BigInteger(1, Base64Util.base64urldecode(d));
    }

    public BigInteger getD() {
        return d;
    }

    public void setD(BigInteger d) {
        this.d = d;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MODULUS, JSONObject.NULL);
        jsonObject.put(EXPONENT, JSONObject.NULL);
        jsonObject.put(D, Base64Util.base64urlencodeUnsignedBigInt(d));

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

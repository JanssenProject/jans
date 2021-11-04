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
 * The Private Key for the RSA Algorithm
 *
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class RSAPrivateKey extends PrivateKey {

    private BigInteger modulus;
    private BigInteger privateExponent;

    public RSAPrivateKey(SignatureAlgorithm signatureAlgorithm, BigInteger modulus, BigInteger privateExponent) {
        setSignatureAlgorithm(signatureAlgorithm);
        this.modulus = modulus;
        this.privateExponent = privateExponent;
    }

    public RSAPrivateKey(SignatureAlgorithm signatureAlgorithm, String modulus, String privateExponent) {
        setSignatureAlgorithm(signatureAlgorithm);
        this.modulus = new BigInteger(1, Base64Util.base64urldecode(modulus));
        this.privateExponent = new BigInteger(1, Base64Util.base64urldecode(privateExponent));
    }

    public BigInteger getModulus() {
        return modulus;
    }

    public void setModulus(BigInteger modulus) {
        this.modulus = modulus;
    }

    public BigInteger getPrivateExponent() {
        return privateExponent;
    }

    public void setPrivateExponent(BigInteger privateExponent) {
        this.privateExponent = privateExponent;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(MODULUS, Base64Util.base64urlencodeUnsignedBigInt(modulus));
        jsonObject.put(EXPONENT, Base64Util.base64urlencodeUnsignedBigInt(privateExponent));
        jsonObject.put(D, JSONObject.NULL);

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

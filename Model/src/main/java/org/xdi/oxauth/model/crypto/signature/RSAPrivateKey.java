/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

import java.math.BigInteger;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.PrivateKey;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * The Private Key for the RSA Algorithm
 *
 * @author Javier Rojas Blum Date: 10.22.2012
 */
public class RSAPrivateKey extends PrivateKey {

    private BigInteger modulus;
    private BigInteger privateExponent;

    public RSAPrivateKey(BigInteger modulus, BigInteger privateExponent) {
        this.modulus = modulus;
        this.privateExponent = privateExponent;
    }

    public RSAPrivateKey(String modulus, String privateExponent) {
        this.modulus = new BigInteger(JwtUtil.base64urldecode(modulus));
        this.privateExponent = new BigInteger(JwtUtil.base64urldecode(privateExponent));
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

        jsonObject.put("modulus", JwtUtil.base64urlencode(modulus.toByteArray()));
        jsonObject.put("privateExponent", JwtUtil.base64urlencode(privateExponent.toByteArray()));
        jsonObject.put("d", JSONObject.NULL);

        return jsonObject;
    }

    @Override
    public String toString() {
        try {
            return toJSONObject().toString(4);
        } catch (JSONException e) {
            return StringUtils.EMPTY_STRING;
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }
}
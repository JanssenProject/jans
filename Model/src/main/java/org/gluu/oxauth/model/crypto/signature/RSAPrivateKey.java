/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.crypto.signature;

import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxauth.model.crypto.PrivateKey;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.model.util.StringUtils;

import static org.gluu.oxauth.model.jwk.JWKParameter.*;

import java.math.BigInteger;

/**
 * The Private Key for the RSA Algorithm
 *
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class RSAPrivateKey extends PrivateKey {

    private BigInteger modulus;
    private BigInteger privateExponent;

    public RSAPrivateKey(BigInteger modulus, BigInteger privateExponent) {
        this.modulus = modulus;
        this.privateExponent = privateExponent;
    }

    public RSAPrivateKey(String modulus, String privateExponent) {
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
        } catch (JSONException e) {
            return StringUtils.EMPTY_STRING;
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }
}
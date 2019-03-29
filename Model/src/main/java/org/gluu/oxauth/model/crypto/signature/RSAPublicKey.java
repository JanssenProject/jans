/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.crypto.signature;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.crypto.PublicKey;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.model.util.StringUtils;

import static org.gluu.oxauth.model.jwk.JWKParameter.*;

import java.math.BigInteger;

/**
 * The Public Key for the RSA Algorithm
 *
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class RSAPublicKey extends PublicKey {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String USE = "sig";

    private BigInteger modulus;
    private BigInteger publicExponent;

    public RSAPublicKey(BigInteger modulus, BigInteger publicExponent) {
        this.modulus = modulus;
        this.publicExponent = publicExponent;
    }

    public RSAPublicKey(String modulus, String publicExponent) {
        this(new BigInteger(1, Base64Util.base64urldecode(modulus)),
                new BigInteger(1, Base64Util.base64urldecode(publicExponent)));
    }

    public BigInteger getModulus() {
        return modulus;
    }

    public void setModulus(BigInteger modulus) {
        this.modulus = modulus;
    }

    public BigInteger getPublicExponent() {
        return publicExponent;
    }

    public void setPublicExponent(BigInteger publicExponent) {
        this.publicExponent = publicExponent;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(MODULUS, Base64Util.base64urlencodeUnsignedBigInt(modulus));
        jsonObject.put(EXPONENT, Base64Util.base64urlencodeUnsignedBigInt(publicExponent));
        jsonObject.put(X, JSONObject.NULL);
        jsonObject.put(Y, JSONObject.NULL);

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
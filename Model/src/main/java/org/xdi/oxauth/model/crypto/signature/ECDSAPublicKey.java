/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.StringUtils;

import java.math.BigInteger;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * The Public Key for the Elliptic Curve Digital Signature Algorithm (ECDSA)
 *
 * @author Javier Rojas Blum
 * @version June 25, 2016
 */
public class ECDSAPublicKey extends PublicKey {

    private static final String ECDSA_ALGORITHM = "EC";
    private static final String USE = "sig";

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
                new BigInteger(1, JwtUtil.base64urldecode(x)),
                new BigInteger(1, JwtUtil.base64urldecode(y)));
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

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
        jsonObject.put(X, JwtUtil.base64urlencodeUnsignedBigInt(x));
        jsonObject.put(Y, JwtUtil.base64urlencodeUnsignedBigInt(y));

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
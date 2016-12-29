/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.oxauth.model.crypto;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxeleven.model.JwksRequestParam;
import org.gluu.oxeleven.model.KeyRequestParam;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithmFamily;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.util.Base64Util;
import sun.security.rsa.RSAPublicKeyImpl;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @version August 17, 2016
 */
public abstract class AbstractCryptoProvider {

    public abstract JSONObject generateKey(SignatureAlgorithm signatureAlgorithm, Long expirationTime) throws Exception;

    public abstract String sign(String signingInput, String keyId, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception;

    public abstract boolean verifySignature(String signingInput, String encodedSignature, String keyId, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception;

    public abstract boolean deleteKey(String keyId) throws Exception;

    public String getKeyId(JSONWebKeySet jsonWebKeySet, SignatureAlgorithm signatureAlgorithm) throws Exception {
        for (JSONWebKey key : jsonWebKeySet.getKeys()) {
            if (signatureAlgorithm == key.getAlg()) {
                return key.getKid();
            }
        }

        return null;
    }

    public JwksRequestParam getJwksRequestParam(JSONObject jwkJsonObject) throws JSONException {
        JwksRequestParam jwks = new JwksRequestParam();
        jwks.setKeyRequestParams(new ArrayList<KeyRequestParam>());

        KeyRequestParam key = new KeyRequestParam();
        key.setAlg(jwkJsonObject.getString(ALGORITHM));
        key.setKid(jwkJsonObject.getString(KEY_ID));
        key.setUse(jwkJsonObject.getString(KEY_USE));
        key.setKty(jwkJsonObject.getString(KEY_TYPE));

        key.setN(jwkJsonObject.optString(MODULUS));
        key.setE(jwkJsonObject.optString(EXPONENT));

        key.setCrv(jwkJsonObject.optString(CURVE));
        key.setX(jwkJsonObject.optString(X));
        key.setY(jwkJsonObject.optString(Y));

        jwks.getKeyRequestParams().add(key);

        return jwks;
    }

    public static JSONObject generateJwks(int keyRegenerationInterval, int idTokenLifeTime, AppConfiguration configuration) throws Exception {
        JSONArray keys = new JSONArray();

        GregorianCalendar expirationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expirationTime.add(GregorianCalendar.HOUR, keyRegenerationInterval);
        expirationTime.add(GregorianCalendar.SECOND, idTokenLifeTime);

        AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(configuration);

        try {
            keys.put(cryptoProvider.generateKey(SignatureAlgorithm.RS256, expirationTime.getTimeInMillis()));
        } catch (Exception ex) {
        }

        try {
            keys.put(cryptoProvider.generateKey(SignatureAlgorithm.RS384, expirationTime.getTimeInMillis()));
        } catch (Exception ex) {
        }

        try {
            keys.put(cryptoProvider.generateKey(SignatureAlgorithm.RS512, expirationTime.getTimeInMillis()));
        } catch (Exception ex) {
        }

        try {
            keys.put(cryptoProvider.generateKey(SignatureAlgorithm.ES256, expirationTime.getTimeInMillis()));
        } catch (Exception ex) {
        }

        try {
            keys.put(cryptoProvider.generateKey(SignatureAlgorithm.ES384, expirationTime.getTimeInMillis()));
        } catch (Exception ex) {
        }

        try {
            keys.put(cryptoProvider.generateKey(SignatureAlgorithm.ES512, expirationTime.getTimeInMillis()));
        } catch (Exception ex) {
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_WEB_KEY_SET, keys);

        return jsonObject;
    }

    public PublicKey getPublicKey(String alias, JSONObject jwks) throws Exception {
        java.security.PublicKey publicKey = null;

        JSONArray webKeys = jwks.getJSONArray(JSON_WEB_KEY_SET);
        for (int i = 0; i < webKeys.length(); i++) {
            JSONObject key = webKeys.getJSONObject(i);
            if (alias.equals(key.getString(KEY_ID))) {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(key.getString(ALGORITHM));
                if (signatureAlgorithm != null) {
                    if (signatureAlgorithm.getFamily().equals(SignatureAlgorithmFamily.RSA)) {
                        publicKey = new RSAPublicKeyImpl(
                                new BigInteger(1, Base64Util.base64urldecode(key.getString(MODULUS))),
                                new BigInteger(1, Base64Util.base64urldecode(key.getString(EXPONENT))));
                    } else if (signatureAlgorithm.getFamily().equals(SignatureAlgorithmFamily.EC)) {
                        AlgorithmParameters parameters = AlgorithmParameters.getInstance(SignatureAlgorithmFamily.EC);
                        parameters.init(new ECGenParameterSpec(signatureAlgorithm.getCurve().getAlias()));
                        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

                        publicKey = KeyFactory.getInstance(SignatureAlgorithmFamily.EC).generatePublic(new ECPublicKeySpec(
                                new ECPoint(
                                        new BigInteger(1, Base64Util.base64urldecode(key.getString(X))),
                                        new BigInteger(1, Base64Util.base64urldecode(key.getString(Y)))
                                ), ecParameters));
                    }
                }
            }
        }

        return publicKey;
    }
}
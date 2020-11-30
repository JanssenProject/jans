/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto;

import com.google.common.collect.Lists;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.ECEllipticCurve;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import org.apache.log4j.Logger;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.util.Base64Util;
import io.jans.eleven.model.JwksRequestParam;
import io.jans.eleven.model.KeyRequestParam;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.jans.as.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public abstract class AbstractCryptoProvider {

    protected static final Logger LOG = Logger.getLogger(AbstractCryptoProvider.class);

    private int keyRegenerationIntervalInDays = -1;

    public JSONObject generateKey(Algorithm algorithm, Long expirationTime) throws Exception {
        return generateKey(algorithm, expirationTime, Use.SIGNATURE);
    }

    public abstract JSONObject generateKey(Algorithm algorithm, Long expirationTime, Use use) throws Exception;

    public abstract String sign(String signingInput, String keyId, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception;

    public abstract boolean verifySignature(String signingInput, String encodedSignature, String keyId, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception;

    public abstract boolean deleteKey(String keyId) throws Exception;

    public abstract boolean containsKey(String keyId);

    public List<String> getKeys() {
        return Lists.newArrayList();
    }

    public abstract PrivateKey getPrivateKey(String keyId) throws Exception;

    public String getKeyId(JSONWebKeySet jsonWebKeySet, Algorithm algorithm, Use use) throws Exception {
        if (algorithm == null || AlgorithmFamily.HMAC.equals(algorithm.getFamily())) {
            return null;
        }
        for (JSONWebKey key : jsonWebKeySet.getKeys()) {
            if (algorithm == key.getAlg() && (use == null || use == key.getUse())) {
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

    public static JSONObject generateJwks(AbstractCryptoProvider cryptoProvider, int keyRegenerationInterval, int idTokenLifeTime, AppConfiguration configuration) throws Exception {
        JSONArray keys = new JSONArray();
        generateJwks(cryptoProvider, keys, keyRegenerationInterval, idTokenLifeTime, configuration, Use.SIGNATURE);
        generateJwks(cryptoProvider, keys, keyRegenerationInterval, idTokenLifeTime, configuration, Use.ENCRYPTION);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSON_WEB_KEY_SET, keys);

        return jsonObject;
    }

    public static void generateJwks(AbstractCryptoProvider cryptoProvider, JSONArray keys, int keyRegenerationInterval, int idTokenLifeTime, AppConfiguration configuration, Use use) throws Exception {
        GregorianCalendar expirationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expirationTime.add(GregorianCalendar.HOUR, keyRegenerationInterval);
        expirationTime.add(GregorianCalendar.SECOND, idTokenLifeTime);

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.RS256, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.RS384, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.RS512, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.ES256, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.ES384, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.ES512, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.PS256, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.PS384, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.PS512, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.RSA1_5, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        try {
            keys.put(cryptoProvider.generateKey(Algorithm.RSA_OAEP, expirationTime.getTimeInMillis(), use));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public PublicKey getPublicKey(String alias, JSONObject jwks, Algorithm requestedAlgorithm) throws Exception {
        java.security.PublicKey publicKey = null;

        JSONArray webKeys = jwks.getJSONArray(JSON_WEB_KEY_SET);
        if (alias == null) {
            if (webKeys.length() == 1) {
                JSONObject key = webKeys.getJSONObject(0);
                return processKey(requestedAlgorithm, alias, key);
            } else {
                return null;
            }
        }
        for (int i = 0; i < webKeys.length(); i++) {
            JSONObject key = webKeys.getJSONObject(i);
            if (alias.equals(key.getString(KEY_ID))) {
                publicKey = processKey(requestedAlgorithm, alias, key);
                if (publicKey != null) {
                    return publicKey;
                }
            }
        }

        return null;
    }

    private PublicKey processKey(Algorithm requestedAlgorithm, String alias, JSONObject key) throws Exception {
        PublicKey publicKey = null;
        AlgorithmFamily family = null;
        if (key.has(ALGORITHM)) {
            Algorithm algorithm = Algorithm.fromString(key.optString(ALGORITHM));

            if (requestedAlgorithm != null && algorithm != requestedAlgorithm) {
                LOG.trace("kid matched but algorithm does not match. kid algorithm:" + algorithm + ", requestedAlgorithm:" + requestedAlgorithm + ", kid:" + alias);
                return null;
            }
            family = algorithm.getFamily();
        } else if (key.has(KEY_TYPE)) {
            family = AlgorithmFamily.fromString(key.getString(KEY_TYPE));
        }

        if (AlgorithmFamily.RSA.equals(family)) {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(
                    new BigInteger(1, Base64Util.base64urldecode(key.getString(MODULUS))),
                    new BigInteger(1, Base64Util.base64urldecode(key.getString(EXPONENT))));
            publicKey = keyFactory.generatePublic(pubKeySpec);
        } else if (AlgorithmFamily.EC.equals(family)) {
            ECEllipticCurve curve = ECEllipticCurve.fromString(key.optString(CURVE));
            AlgorithmParameters parameters = AlgorithmParameters.getInstance(AlgorithmFamily.EC.toString());
            parameters.init(new ECGenParameterSpec(curve.getAlias()));
            ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

            publicKey = KeyFactory.getInstance(AlgorithmFamily.EC.toString()).generatePublic(new ECPublicKeySpec(
                    new ECPoint(
                            new BigInteger(1, Base64Util.base64urldecode(key.getString(X))),
                            new BigInteger(1, Base64Util.base64urldecode(key.getString(Y)))
                    ), ecParameters));
        }

        if (key.has(EXPIRATION_TIME)) {
            checkKeyExpiration(alias, key.getLong(EXPIRATION_TIME));
        }

        return publicKey;
    }

    protected void checkKeyExpiration(String alias, Long expirationTime) {
        try {
            Date expirationDate = new Date(expirationTime);
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date today = new Date();
            long expiresInDays = (expirationTime - today.getTime()) / (24 * 60 * 60 * 1000);
            if (expiresInDays == 0) {
                LOG.warn("\nWARNING! Key will expire soon, alias: " + alias
                        + "\n\tExpires On: " + ft.format(expirationDate)
                        + "\n\tToday's Date: " + ft.format(today));
                return;
            }
            if (expiresInDays < 0) {
                LOG.warn("\nWARNING! Expired Key is used, alias: " + alias
                        + "\n\tExpires On: " + ft.format(expirationDate)
                        + "\n\tToday's Date: " + ft.format(today));
                return;
            }

            // re-generation interval is unknown, therefore we default to 30 days period warning
            if (keyRegenerationIntervalInDays <= 0 && expiresInDays < 30) {
                LOG.warn("\nWARNING! Key with alias: " + alias
                        + "\n\tExpires In: " + expiresInDays + " days"
                        + "\n\tExpires On: " + ft.format(expirationDate)
                        + "\n\tToday's Date: " + ft.format(today));
                return;
            }

            if (expiresInDays < keyRegenerationIntervalInDays) {
                LOG.warn("\nWARNING! Key with alias: " + alias
                        + "\n\tExpires In: " + expiresInDays + " days"
                        + "\n\tExpires On: " + ft.format(expirationDate)
                        + "\n\tKey Regeneration In: " + keyRegenerationIntervalInDays + " days"
                        + "\n\tToday's Date: " + ft.format(today));
            }
        } catch (Exception e) {
            LOG.error("Failed to check key expiration.", e);
        }
    }

    public int getKeyRegenerationIntervalInDays() {
        return keyRegenerationIntervalInDays;
    }

    public void setKeyRegenerationIntervalInDays(int keyRegenerationIntervalInDays) {
        this.keyRegenerationIntervalInDays = keyRegenerationIntervalInDays;
    }
}
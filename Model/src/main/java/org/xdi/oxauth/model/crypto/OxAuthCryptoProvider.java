/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.signature.*;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.model.util.JwtUtil;

import java.math.BigInteger;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @version April 25, 2016
 */
public class OxAuthCryptoProvider extends AbstractCryptoProvider {

    private JSONWebKeySet webKeySet;

    public OxAuthCryptoProvider(JSONWebKeySet webKeySet) {
        this.webKeySet = webKeySet;
    }

    @Override
    public String generateKey(SignatureAlgorithm signatureAlgorithm) throws Exception {
        return null;
    }

    @Override
    public String sign(String signingInput, String alias, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception {
        Signer signer = null;
        JSONWebKey webKey = null;

        switch (signatureAlgorithm) {
            case NONE:
                signer = new NoneSigner(signatureAlgorithm);
                break;
            case HS256:
            case HS384:
            case HS512:
                signer = new HMACSigner(signatureAlgorithm, sharedSecret);
                break;
            case RS256:
            case RS384:
            case RS512:
                webKey = webKeySet.getKey(alias);
                RSAPrivateKey rsaPrivateKey = new RSAPrivateKey(
                        webKey.getPrivateKey().getN(),
                        webKey.getPrivateKey().getE());
                signer = new RSASigner(signatureAlgorithm, rsaPrivateKey);
                break;
            case ES256:
            case ES384:
            case ES512:
                webKey = webKeySet.getKey(alias);
                ECDSAPrivateKey ecdsaPrivateKey = new ECDSAPrivateKey(webKey.getPrivateKey().getD());
                signer = new ECSigner(signatureAlgorithm, ecdsaPrivateKey);
                break;
            default:
                throw new Exception("Invalid signature algorithm");
        }

        return signer.sign(signingInput);
    }

    @Override
    public boolean verifySignature(String signingInput, String signature, String alias, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception {
        Signer signer = null;

        switch (signatureAlgorithm) {
            case NONE:
                signer = new NoneSigner(signatureAlgorithm);
                break;
            case HS256:
            case HS384:
            case HS512:
                signer = new HMACSigner(signatureAlgorithm, sharedSecret);
                break;
            case RS256:
            case RS384:
            case RS512:
                RSAPublicKey rsaPublicKey = new RSAPublicKey(
                        new BigInteger(1, JwtUtil.base64urldecode(jwks.getString(MODULUS))),
                        new BigInteger(1, JwtUtil.base64urldecode(jwks.getString(EXPONENT)))
                );
                signer = new RSASigner(signatureAlgorithm, rsaPublicKey);
                break;
            case ES256:
            case ES384:
            case ES512:
                ECDSAPublicKey ecdsaPublicKey = new ECDSAPublicKey(signatureAlgorithm,
                        new BigInteger(1, JwtUtil.base64urldecode(jwks.getString(X))),
                        new BigInteger(1, JwtUtil.base64urldecode(jwks.getString(Y))));
                signer = new ECSigner(signatureAlgorithm, ecdsaPublicKey);
                break;
            default:
                throw new Exception("Invalid signature algorithm");
        }

        return signer.verifySignature(signingInput, signature);
    }

    @Override
    public boolean deleteKey(String alias) throws Exception {
        return false;
    }

    @Override
    public JSONObject jwks(JSONWebKeySet jsonWebKeySet) throws Exception {
        JSONObject jsonObj = new JSONObject();
        JSONArray keys = new JSONArray();

        for (JSONWebKey jsonWebKey : jsonWebKeySet.getKeys()) {
            JSONObject jsonKeyValue = new JSONObject();
            if (jsonWebKey.getKty() != null) {
                jsonKeyValue.put(KEY_TYPE, jsonWebKey.getKty());
            }
            if (jsonWebKey.getKid() != null) {
                jsonKeyValue.put(KEY_ID, jsonWebKey.getKid());
            }
                /*if (ConfigurationFactory.instance().getConfiguration().getKeyRegenerationEnabled()
                        && jsonWebKey.getExp() != null) {
                    jsonKeyValue.put(EXPIRATION_TIME, jsonWebKey.getExp());
                }*/
            if (jsonWebKey.getUse() != null) {
                jsonKeyValue.put(KEY_USE, jsonWebKey.getUse());
            }
            if (jsonWebKey.getAlg() != null) {
                jsonKeyValue.put(ALGORITHM, jsonWebKey.getAlg());
            }
            if (jsonWebKey.getCrv() != null) {
                jsonKeyValue.put(CURVE, jsonWebKey.getCrv());
            }
            if (jsonWebKey.getPublicKey() != null) {
                if (jsonWebKey.getPublicKey().getN() != null) {
                    jsonKeyValue.put(MODULUS, jsonWebKey.getPublicKey().getN());
                }
                if (jsonWebKey.getPublicKey().getE() != null) {
                    jsonKeyValue.put(EXPONENT, jsonWebKey.getPublicKey().getE());
                }
                if (jsonWebKey.getPublicKey().getX() != null) {
                    jsonKeyValue.put(X, jsonWebKey.getPublicKey().getX());
                }
                if (jsonWebKey.getPublicKey().getY() != null) {
                    jsonKeyValue.put(Y, jsonWebKey.getPublicKey().getY());
                }
                if (jsonWebKey.getX5c() != null) {
                    //jsonKeyValue.put(X5C, new JSONArray(jsonWebKey.getCertificateChain()));
                }

                keys.put(jsonKeyValue);
            }
        }

        jsonObj.put(JSON_WEB_KEY_SET, keys);
        return jsonObj;
    }
}
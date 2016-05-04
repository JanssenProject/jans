/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.oxauth.model.crypto;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxeleven.model.JwksRequestParam;
import org.gluu.oxeleven.model.KeyRequestParam;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwk.JSONWebKey;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;

import java.util.ArrayList;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * @author Javier Rojas Blum
 * @version May 4, 2016
 */
public abstract class AbstractCryptoProvider {

    public abstract JSONObject generateKey(SignatureAlgorithm signatureAlgorithm, Long expirationTime) throws Exception;

    public abstract String sign(String signingInput, String keyId, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception;

    public abstract boolean verifySignature(String signingInput, String signature, String keyId, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception;

    public abstract boolean deleteKey(String keyId) throws Exception;

    public abstract JSONObject jwks(JSONWebKeySet jsonWebKeySet) throws Exception;

    public String getKeyId(JSONWebKeySet jsonWebKeySet, SignatureAlgorithm signatureAlgorithm) throws Exception {
        JSONObject jwks = jwks(jsonWebKeySet);

        for (int i = 0; i < jwks.getJSONArray(JSON_WEB_KEY_SET).length(); i++) {
            JSONObject key = jwks.getJSONArray(JSON_WEB_KEY_SET).getJSONObject(i);
            if (signatureAlgorithm.getName().equals(key.optString(ALGORITHM))) {
                return key.optString(KEY_ID);
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

    public JwksRequestParam getJwksRequestParam(JSONWebKeySet jsonWebKeySet) {
        JwksRequestParam jwks = new JwksRequestParam();
        jwks.setKeyRequestParams(new ArrayList<KeyRequestParam>());
        for (JSONWebKey jsonWebKey : jsonWebKeySet.getKeys()) {
            KeyRequestParam key = new KeyRequestParam();
            key.setAlg(jsonWebKey.getAlg());
            key.setKid(jsonWebKey.getKid());
            key.setUse(jsonWebKey.getUse().toValue());
            key.setKty(jsonWebKey.getKty().toValue());
            key.setCrv(jsonWebKey.getCrv());

            jwks.getKeyRequestParams().add(key);
        }

        return jwks;
    }
}
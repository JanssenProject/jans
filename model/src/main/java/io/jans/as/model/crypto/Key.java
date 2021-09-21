/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto;

import io.jans.as.model.common.JSONable;
import io.jans.as.model.util.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import static io.jans.as.model.jwk.JWKParameter.ALGORITHM;
import static io.jans.as.model.jwk.JWKParameter.CERTIFICATE_CHAIN;
import static io.jans.as.model.jwk.JWKParameter.CURVE;
import static io.jans.as.model.jwk.JWKParameter.EXPIRATION_TIME;
import static io.jans.as.model.jwk.JWKParameter.KEY_ID;
import static io.jans.as.model.jwk.JWKParameter.KEY_TYPE;
import static io.jans.as.model.jwk.JWKParameter.KEY_USE;
import static io.jans.as.model.jwk.JWKParameter.PRIVATE_KEY;
import static io.jans.as.model.jwk.JWKParameter.PUBLIC_KEY;

/**
 * @author Javier Rojas Blum
 * @version February 17, 2016
 */
public class Key<E extends PrivateKey, F extends PublicKey> implements JSONable {

    private String keyType;
    private String use;
    private String algorithm;
    private String keyId;
    private Long expirationTime;
    private Object curve;
    private E privateKey;
    private F publicKey;
    private Certificate certificate;

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Object getCurve() {
        return curve;
    }

    public void setCurve(Object curve) {
        this.curve = curve;
    }

    public E getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(E privateKey) {
        this.privateKey = privateKey;
    }

    public F getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(F publicKey) {
        this.publicKey = publicKey;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(KEY_TYPE, getKeyType());
        jsonObject.put(KEY_USE, getUse());
        jsonObject.put(ALGORITHM, getAlgorithm());
        jsonObject.put(KEY_ID, getKeyId());
        jsonObject.put(EXPIRATION_TIME, getExpirationTime() == null ? JSONObject.NULL : getExpirationTime());
        jsonObject.put(CURVE, getCurve());
        jsonObject.put(PRIVATE_KEY, getPrivateKey().toJSONObject());
        jsonObject.put(PUBLIC_KEY, getPublicKey().toJSONObject());
        jsonObject.put(CERTIFICATE_CHAIN, getCertificate().toJSONArray());

        return jsonObject;
    }

    @Override
    public String toString() {
        try {
            return toJSONObject().toString(4).replace("\\/", "/");
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }
}
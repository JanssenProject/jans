/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

import java.util.List;

/**
 * @author Javier Rojas Blum Date: 11.15.2011
 */
public class JSONWebKeySet {

    private List<JSONWebKey> keys;

    public List<JSONWebKey> getKeys() {
        return keys;
    }

    public void setKeys(List<JSONWebKey> keys) {
        this.keys = keys;
    }

    public JSONWebKey getKey(String keyId) {
        for (JSONWebKey JSONWebKey : keys) {
            if (JSONWebKey.getKeyId().equals(keyId)) {
                return JSONWebKey;
            }
        }

        return null;
    }

    public JSONWebKey getKeyByAlgorithm(String algorithm) {
        for (JSONWebKey JSONWebKey : keys) {
            if (JSONWebKey.getAlgorithm().equals(algorithm)) {
                return JSONWebKey;
            }
        }

        return null;
    }
}
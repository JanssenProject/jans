/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithmFamily;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version 0.9 December 9, 2014
 */
public class JSONWebKeySet {

    private List<JSONWebKey> keys;

    /*public JSONWebKeySet(List<JSONWebKey> keys){
        this.keys=keys;
    }*/

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

    public List<JSONWebKey> getKeys(SignatureAlgorithm algorithm) {
        List<JSONWebKey> jsonWebKeys = new ArrayList<JSONWebKey>();

        if (SignatureAlgorithmFamily.RSA.equals(algorithm.getFamily())) {
            for (JSONWebKey JSONWebKey : keys) {
                if (JSONWebKey.getAlgorithm().equals(algorithm.getName())) {
                    jsonWebKeys.add(JSONWebKey);
                }
            }
        } else if (SignatureAlgorithmFamily.EC.equals(algorithm.getFamily())) {
            for (JSONWebKey JSONWebKey : keys) {
                if (JSONWebKey.getCurve().equals(algorithm.getCurve())) {
                    jsonWebKeys.add(JSONWebKey);
                }
            }
        }

        Collections.sort(jsonWebKeys);
        return jsonWebKeys;
    }
}
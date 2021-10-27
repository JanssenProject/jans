/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.jans.as.model.crypto.PublicKey;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.KeySelectionStrategy;

/**
 * Represents a JSON Web Key (JWK) received from the authorization server.
 *
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class JwkResponse extends BaseResponse {

    private JSONWebKeySet jwks;

    /**
     * Constructs a JWK response.
     *
     * @param status The response status code.
     */
    public JwkResponse(int status) {
        super(status);
    }

    public JSONWebKeySet getJwks() {
        return jwks;
    }

    public void setJwks(JSONWebKeySet jwks) {
        this.jwks = jwks;
    }

    /**
     * Search and returns a {@link JSONWebKey} given its <code>keyId</code>.
     *
     * @param keyId The key id.
     * @return The JSONWebKey if found, otherwise <code>null</code>.
     */
    @Deprecated
    public JSONWebKey getKeyValue(String keyId) {
        for (JSONWebKey JSONWebKey : jwks.getKeys()) {
            if (JSONWebKey.getKid().equals(keyId)) {
                return JSONWebKey;
            }
        }

        return null;
    }

    @Deprecated
    public PublicKey getPublicKey(String keyId) {
        PublicKey publicKey = null;
        JSONWebKey JSONWebKey = getKeyValue(keyId);

        if (JSONWebKey != null) {
            switch (JSONWebKey.getKty()) {
                case RSA:
                    publicKey = new RSAPublicKey(
                            JSONWebKey.getN(),
                            JSONWebKey.getE());
                    break;
                case EC:
                    publicKey = new ECDSAPublicKey(
                            SignatureAlgorithm.fromString(JSONWebKey.getAlg().getParamName()),
                            JSONWebKey.getX(),
                            JSONWebKey.getY());
                    break;
                default:
                    break;
            }
        }

        return publicKey;
    }

    public List<JSONWebKey> getKeys(Algorithm algorithm) {
        List<JSONWebKey> jsonWebKeys = new ArrayList<JSONWebKey>();

        if (AlgorithmFamily.RSA.equals(algorithm.getFamily())) {
            for (JSONWebKey jsonWebKey : jwks.getKeys()) {
                if (jsonWebKey.getAlg().equals(algorithm)) {
                    jsonWebKeys.add(jsonWebKey);
                }
            }
        } else if (AlgorithmFamily.EC.equals(algorithm.getFamily())) {
            for (JSONWebKey jsonWebKey : jwks.getKeys()) {
                if (jsonWebKey.getAlg().equals(algorithm)) {
                    jsonWebKeys.add(jsonWebKey);
                }
            }
        }

        Collections.sort(jsonWebKeys, KeySelectionStrategy.compareExp());
        return jsonWebKeys;
    }

    public String getKeyId(Algorithm algorithm) {
        List<JSONWebKey> jsonWebKeys = getKeys(algorithm);
        if (jsonWebKeys.size() > 0) {
            return jsonWebKeys.get(0).getKid();
        } else {
            return null;
        }
    }
}
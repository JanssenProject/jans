/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithmFamily;
import org.xdi.oxauth.model.jwk.JSONWebKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a JSON Web Key (JWK) received from the authorization server.
 *
 * @author Javier Rojas Blum
 * @version February 17, 2016
 */
public class JwkResponse extends BaseResponse {

    private List<JSONWebKey> keys;

    /**
     * Constructs a JWK response.
     *
     * @param status The response status code.
     */
    public JwkResponse(int status) {
        super(status);
    }

    /**
     * Returns the list of key values.
     *
     * @return The list of key values.
     */
    public List<JSONWebKey> getKeys() {
        return keys;
    }

    /**
     * Sets the list of key values.
     *
     * @param keys The list of key values.
     */
    public void setKeys(List<JSONWebKey> keys) {
        this.keys = keys;
    }

    /**
     * Search and returns a {@link org.xdi.oxauth.model.jwk.JSONWebKey} given its <code>keyId</code>.
     *
     * @param keyId The key id.
     * @return The JSONWebKey if found, otherwise <code>null</code>.
     */
    public JSONWebKey getKeyValue(String keyId) {
        for (JSONWebKey JSONWebKey : keys) {
            if (JSONWebKey.getKid().equals(keyId)) {
                return JSONWebKey;
            }
        }

        return null;
    }

    public PublicKey getPublicKey(String keyId) {
        PublicKey publicKey = null;
        JSONWebKey JSONWebKey = getKeyValue(keyId);

        if (JSONWebKey != null) {
            if (JSONWebKey.getPublicKey() != null) {
                switch (JSONWebKey.getKty()) {
                    case RSA:
                        publicKey = new RSAPublicKey(
                                JSONWebKey.getPublicKey().getN(),
                                JSONWebKey.getPublicKey().getE());
                        break;
                    case EC:
                        publicKey = new ECDSAPublicKey(
                                SignatureAlgorithm.fromName(JSONWebKey.getCrv()),
                                JSONWebKey.getPublicKey().getX(),
                                JSONWebKey.getPublicKey().getY());
                        break;
                    default:
                        break;
                }
            }
        }

        return publicKey;
    }

    public List<JSONWebKey> getKeys(SignatureAlgorithm algorithm) {
        List<JSONWebKey> jsonWebKeys = new ArrayList<JSONWebKey>();

        if (SignatureAlgorithmFamily.RSA.equals(algorithm.getFamily())) {
            for (JSONWebKey jsonWebKey : keys) {
                if (jsonWebKey.getAlg().equals(algorithm.getName())) {
                    jsonWebKeys.add(jsonWebKey);
                }
            }
        } else if (SignatureAlgorithmFamily.EC.equals(algorithm.getFamily())) {
            for (JSONWebKey jsonWebKey : keys) {
                if (jsonWebKey.getAlg().equals(algorithm.getName())) {
                    jsonWebKeys.add(jsonWebKey);
                }
            }
        }

        Collections.sort(jsonWebKeys);
        return jsonWebKeys;
    }

    public String getKeyId(SignatureAlgorithm signatureAlgorithm) {
        List<JSONWebKey> jsonWebKeys = getKeys(SignatureAlgorithm.RS256);
        if (jsonWebKeys.size() > 0) {
            return jsonWebKeys.get(0).getKid();
        } else {
            return null;
        }
    }
}
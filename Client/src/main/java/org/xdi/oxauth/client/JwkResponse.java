/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import java.util.List;

import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwk.JSONWebKey;

/**
 * Represents a JSON Web Key (JWK) received from the authorization server.
 *
 * @author Javier Rojas Blum Date: 11.15.2011
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
            if (JSONWebKey.getKeyId().equals(keyId)) {
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
                switch (JSONWebKey.getKeyType()) {
                    case RSA:
                        publicKey = new RSAPublicKey(
                                JSONWebKey.getPublicKey().getModulus(),
                                JSONWebKey.getPublicKey().getExponent());
                        break;
                    case EC:
                        publicKey = new ECDSAPublicKey(
                                SignatureAlgorithm.fromName(JSONWebKey.getCurve()),
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
}
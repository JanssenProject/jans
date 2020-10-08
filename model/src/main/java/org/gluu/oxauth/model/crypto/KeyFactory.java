/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.crypto;

/**
 * Factory to create asymmetric Public and Private Keys
 *
 * @author Javier Rojas Blum Date: 10.22.2012
 */
public abstract class KeyFactory<E extends PrivateKey, F extends PublicKey> {

    public abstract E getPrivateKey();

    public abstract F getPublicKey();

    public abstract Certificate getCertificate();

    public Key<E, F> getKey() {
        Key key = new Key();

        key.setPrivateKey(getPrivateKey());
        key.setPublicKey(getPublicKey());
        key.setCertificate(getCertificate());

        return key;
    }
}
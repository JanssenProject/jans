/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto;

/**
 * Factory to create asymmetric Public and Private Keys
 *
 * @author Javier Rojas Blum Date: 10.22.2012
 */
public abstract class KeyFactory<E extends PrivateKey, F extends PublicKey> {

    public static final String DEF_BC = "BC";

    public abstract E getPrivateKey();

    public abstract F getPublicKey();

    public abstract Certificate getCertificate();

    public Key<E, F> getKey() {
        Key<E, F> key = new Key<>();

        key.setPrivateKey(getPrivateKey());
        key.setPublicKey(getPublicKey());
        key.setCertificate(getCertificate());

        return key;
    }
}
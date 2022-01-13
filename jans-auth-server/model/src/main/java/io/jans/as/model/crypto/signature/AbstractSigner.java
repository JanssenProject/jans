/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

/**
 * @author Javier Rojas Blum
 * @version April 22, 2016
 */
public abstract class AbstractSigner implements Signer {

    private final SignatureAlgorithm signatureAlgorithm;

    protected AbstractSigner(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
}

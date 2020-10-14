/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

/**
 * @author Javier Rojas Blum
 * @version April 22, 2016
 */
public abstract class AbstractSigner implements Signer {

    private SignatureAlgorithm signatureAlgorithm;

    protected AbstractSigner(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
}

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto;

import io.jans.as.model.common.JSONable;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;

/**
 * The Public Key for Cryptography algorithms
 *
 * @author Javier Rojas Blum
 * @version June 25, 2016
 */
public abstract class PublicKey implements JSONable {

    private String keyId;

    private SignatureAlgorithm signatureAlgorithm;

    private Certificate certificate;
    
    protected PublicKey (final String keyId, final SignatureAlgorithm signatureAlgorithm, final Certificate certificate) {
        this.keyId = keyId;
        this.signatureAlgorithm = signatureAlgorithm;
        this.certificate = certificate;
    }    

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }
}
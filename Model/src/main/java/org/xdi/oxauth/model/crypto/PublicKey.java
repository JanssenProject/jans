/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto;

import org.xdi.oxauth.model.common.JSONable;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;

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
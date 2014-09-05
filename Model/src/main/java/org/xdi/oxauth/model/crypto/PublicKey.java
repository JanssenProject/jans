/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto;

import org.xdi.oxauth.model.common.JSONable;

/**
 * The Public Key for Cryptography algorithms
 *
 * @author Javier Rojas Blum Date: 10.22.2012
 */
public abstract class PublicKey implements JSONable {

    private Certificate certificate;

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }
}
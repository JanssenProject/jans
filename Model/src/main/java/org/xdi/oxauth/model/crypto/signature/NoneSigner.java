/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

/**
 * @author Javier Rojas Blum
 * @version April 22, 2016
 */
public class NoneSigner extends AbstractSigner {

    private static final String EMPTY = "";

    public NoneSigner(SignatureAlgorithm signatureAlgorithm) throws Exception {
        super(signatureAlgorithm);

        if (signatureAlgorithm == null || SignatureAlgorithm.NONE != signatureAlgorithm) {
            throw new Exception("Invalid signature algorithm");
        }
    }

    @Override
    public String sign(String signingInput) {
        return EMPTY;
    }

    @Override
    public boolean verifySignature(String signingInput, String signature) {
        return EMPTY.equals(signature);
    }
}

/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jws;

import java.security.SignatureException;

import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * @author Javier Rojas Blum Date: 11.12.2012
 */
public class PlainTextSignature extends AbstractJwsSigner {

    public PlainTextSignature() {
        super(SignatureAlgorithm.NONE);
    }

    @Override
    public String generateSignature(String signingInput) throws SignatureException {
        return StringUtils.EMPTY_STRING;
    }

    @Override
    public boolean validateSignature(String signingInput, String signature) throws SignatureException {
        return false;
    }
}
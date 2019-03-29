/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jws;

import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.util.StringUtils;

import java.security.SignatureException;

/**
 * @author Javier Rojas Blum
 * @version October 26, 2017
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
        return StringUtils.EMPTY_STRING.equals(signature);
    }
}
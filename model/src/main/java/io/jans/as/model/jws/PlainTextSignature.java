/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jws;

import java.security.SignatureException;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.util.StringUtils;

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
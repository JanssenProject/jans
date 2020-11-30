/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jws;

import java.security.SignatureException;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;

/**
 * @author Javier Rojas Blum
 * @version February 8, 2019
 */
public interface JwsSigner {

    SignatureAlgorithm getSignatureAlgorithm();

    Jwt sign(Jwt jwt) throws InvalidJwtException, SignatureException;

    boolean validate(Jwt jwt);
}
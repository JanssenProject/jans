/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jws;

import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.Jwt;

import java.security.SignatureException;

/**
 * @author Javier Rojas Blum
 * @version February 8, 2019
 */
public interface JwsSigner {

    SignatureAlgorithm getSignatureAlgorithm();

    Jwt sign(Jwt jwt) throws InvalidJwtException, SignatureException;

    boolean validate(Jwt jwt);
}
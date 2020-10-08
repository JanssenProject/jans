/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jws;

import java.security.SignatureException;

import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;

/**
 * @author Javier Rojas Blum
 * @version February 8, 2019
 */
public interface JwsSigner {

    SignatureAlgorithm getSignatureAlgorithm();

    Jwt sign(Jwt jwt) throws InvalidJwtException, SignatureException;

    boolean validate(Jwt jwt);
}
/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jws;

import java.security.SignatureException;

import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.Jwt;

/**
 * @author Javier Rojas Blum Date: 11.12.2012
 */
public interface JwsSigner {

    public SignatureAlgorithm getSignatureAlgorithm();

    public Jwt sign(Jwt jwt) throws InvalidJwtException, SignatureException;

    public boolean validate(Jwt jwt);
}
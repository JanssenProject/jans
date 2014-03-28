package org.xdi.oxauth.model.jws;

import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.Jwt;

import java.security.SignatureException;

/**
 * @author Javier Rojas Blum Date: 11.12.2012
 */
public interface JwsSigner {

    public SignatureAlgorithm getSignatureAlgorithm();

    public Jwt sign(Jwt jwt) throws InvalidJwtException, SignatureException;

    public boolean validate(Jwt jwt);
}
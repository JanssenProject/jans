/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jws;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.util.HashUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.security.SignatureException;

/**
 * @author Javier Rojas Blum
 * @version March 14, 2019
 */
public abstract class AbstractJwsSigner implements JwsSigner {

    private static final Logger LOG = Logger.getLogger(AbstractJwsSigner.class);

    private SignatureAlgorithm signatureAlgorithm;

    protected AbstractJwsSigner(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    @Override
    public Jwt sign(Jwt jwt) throws InvalidJwtException, SignatureException {
        String signature = generateSignature(jwt.getSigningInput());
        jwt.setEncodedSignature(signature);
        return jwt;
    }

    @Override
    public boolean validate(Jwt jwt) {
        try {
            String signingInput = jwt.getSigningInput();
            String signature = jwt.getEncodedSignature();

            return validateSignature(signingInput, signature);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean validateAuthorizationCode(String authorizationCode, Jwt idToken) {
        return validateHash(authorizationCode, idToken.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
    }

    public boolean validateAccessToken(String accessToken, Jwt idToken) {
        return validateHash(accessToken, idToken.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
    }

    public boolean validateState(String state, Jwt idToken) {
        return validateHash(state, idToken.getClaims().getClaimAsString(JwtClaimName.STATE_HASH));
    }

    private boolean validateHash(String tokenCode, String tokenHash) {
        if (StringUtils.isBlank(tokenCode) || StringUtils.isBlank(tokenHash)) {
            return false;
        }

        return tokenHash.equals(HashUtil.getHash(tokenCode, signatureAlgorithm));
    }

    public abstract String generateSignature(String signingInput) throws SignatureException;

    public abstract boolean validateSignature(String signingInput, String signature) throws SignatureException;
}
/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jws;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.model.util.JwtUtil;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

/**
 * @author Javier Rojas Blum
 * @version March 14, 2019
 */
public abstract class AbstractJwsSigner implements JwsSigner {

    private static final Logger LOG = Logger.getLogger(AbstractJwsSigner.class);

    private SignatureAlgorithm signatureAlgorithm;

    public AbstractJwsSigner(SignatureAlgorithm signatureAlgorithm) {
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
        } catch (InvalidJwtException e) {
            LOG.error(e.getMessage(), e);
            return false;
        } catch (SignatureException e) {
            LOG.error(e.getMessage(), e);
            return false;
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
        boolean result = false;

        try {
            if (signatureAlgorithm != null
                    && StringUtils.isNotBlank(tokenCode)
                    && StringUtils.isNotBlank(tokenHash)) {
                byte[] digest = null;
                if (signatureAlgorithm == SignatureAlgorithm.HS256 ||
                        signatureAlgorithm == SignatureAlgorithm.RS256 ||
                        signatureAlgorithm == SignatureAlgorithm.ES256) {
                    digest = JwtUtil.getMessageDigestSHA256(tokenCode);
                } else if (signatureAlgorithm == SignatureAlgorithm.HS384 ||
                        signatureAlgorithm == SignatureAlgorithm.RS384 ||
                        signatureAlgorithm == SignatureAlgorithm.ES512) {
                    digest = JwtUtil.getMessageDigestSHA384(tokenCode);
                } else if (signatureAlgorithm == SignatureAlgorithm.HS512 ||
                        signatureAlgorithm == SignatureAlgorithm.RS384 ||
                        signatureAlgorithm == SignatureAlgorithm.ES512) {
                    digest = JwtUtil.getMessageDigestSHA512(tokenCode);
                }

                if (digest != null) {
                    byte[] lefMostHalf = new byte[digest.length / 2];
                    System.arraycopy(digest, 0, lefMostHalf, 0, lefMostHalf.length);
                    String hash = Base64Util.base64urlencode(lefMostHalf);

                    result = hash.equals(tokenHash);
                }
            }
        } catch (NoSuchProviderException e) {
            LOG.error(e.getMessage(), e);
            result = false;
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage(), e);
            result = false;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            result = false;
        }

        return result;
    }

    public abstract String generateSignature(String signingInput) throws SignatureException;

    public abstract boolean validateSignature(String signingInput, String signature) throws SignatureException;
}
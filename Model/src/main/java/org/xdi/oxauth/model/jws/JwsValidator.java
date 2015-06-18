/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jws;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.util.JwtUtil;

/**
 * @author Javier Rojas Blum
 * @version 0.9 May 18, 2015
 */
public class JwsValidator {

    private Jwt jwt;
    private String sharedKey;
    private PublicKey publicKey;

    public JwsValidator(Jwt jwt, String sharedKey) {
        this.jwt = jwt;
        this.sharedKey = sharedKey;
    }

    public JwsValidator(Jwt jwt, String sharedKey, String jwksUri, String jwks) {
        this.jwt = jwt;
        this.sharedKey = sharedKey;

        if (jwt != null) {
            SignatureAlgorithm algorithm = jwt.getHeader().getAlgorithm();
            if (StringUtils.isNotBlank(jwksUri) || StringUtils.isNotBlank(jwks)) {
                String keyId = jwt.getHeader().getKeyId();
                publicKey = JwtUtil.getPublicKey(jwksUri, jwks, algorithm, keyId);
            }
        }
    }

    public boolean validateSignature() {
        boolean validSignature = false;

        if (jwt != null) {
            SignatureAlgorithm algorithm = jwt.getHeader().getAlgorithm();

            if (algorithm == SignatureAlgorithm.NONE) {
                validSignature = StringUtils.isBlank(jwt.getEncodedSignature());
            } else if (algorithm == SignatureAlgorithm.HS256 || algorithm == SignatureAlgorithm.HS384 || algorithm == SignatureAlgorithm.HS512) {
                if (StringUtils.isNotBlank(sharedKey)) {
                    HMACSigner hmacSigner = new HMACSigner(algorithm, sharedKey);
                    validSignature = hmacSigner.validate(jwt);
                }
            } else if (algorithm == SignatureAlgorithm.RS256 || algorithm == SignatureAlgorithm.RS384 || algorithm == SignatureAlgorithm.RS512) {
                if (publicKey != null && publicKey instanceof RSAPublicKey) {
                    RSASigner rsaSigner = new RSASigner(algorithm, (RSAPublicKey) publicKey);
                    validSignature = rsaSigner.validate(jwt);
                }
            } else if (algorithm == SignatureAlgorithm.ES256 || algorithm == SignatureAlgorithm.ES384 || algorithm == SignatureAlgorithm.ES512) {
                if (publicKey != null && publicKey instanceof ECDSAPublicKey) {
                    ECDSASigner ecdsaSigner = new ECDSASigner(algorithm, (ECDSAPublicKey) publicKey);
                    validSignature = ecdsaSigner.validate(jwt);
                }
            }
        }

        return validSignature;
    }
}
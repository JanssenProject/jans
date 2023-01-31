/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jws;

import io.jans.as.model.crypto.signature.RSAPrivateKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.SecurityProviderUtility;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * @author Javier Rojas Blum
 * @version February 8, 2019
 */
public class RSASigner extends AbstractJwsSigner {

    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;

    public RSASigner(SignatureAlgorithm signatureAlgorithm, RSAPrivateKey rsaPrivateKey) {
        super(signatureAlgorithm);
        this.rsaPrivateKey = rsaPrivateKey;
    }

    public RSASigner(SignatureAlgorithm signatureAlgorithm, RSAPublicKey rsaPublicKey) {
        super(signatureAlgorithm);
        this.rsaPublicKey = rsaPublicKey;
    }

    public RSASigner(SignatureAlgorithm signatureAlgorithm, io.jans.as.model.crypto.Certificate certificate) {
        super(signatureAlgorithm);
        this.rsaPublicKey = certificate.getRsaPublicKey();
    }

    @Override
    public String generateSignature(String signingInput) throws SignatureException {
        if (getSignatureAlgorithm() == null) {
            throw new SignatureException("The signature algorithm is null");
        }
        if (rsaPrivateKey == null) {
            throw new SignatureException("The RSA private key is null");
        }
        if (signingInput == null) {
            throw new SignatureException("The signing input is null");
        }

        try {
            RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPrivateExponent());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA", SecurityProviderUtility.getBCProvider());
            PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

            Signature signature = Signature.getInstance(getSignatureAlgorithm().getAlgorithm(), SecurityProviderUtility.getBCProvider());
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));

            return Base64Util.base64urlencode(signature.sign());
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }

    @Override
    public boolean validateSignature(String signingInput, String signature) throws SignatureException {
        if (getSignatureAlgorithm() == null) {
            throw new SignatureException("The signature algorithm is null");
        }
        if (rsaPublicKey == null) {
            throw new SignatureException("The RSA public key is null");
        }
        if (signingInput == null) {
            throw new SignatureException("The signing input is null");
        }

        try {
            byte[] sigBytes = Base64Util.base64urldecode(signature);
            byte[] sigInBytes = signingInput.getBytes(StandardCharsets.UTF_8);

            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(
                    rsaPublicKey.getModulus(),
                    rsaPublicKey.getPublicExponent());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA", SecurityProviderUtility.getBCProvider());
            PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

            Signature sign = Signature.getInstance(getSignatureAlgorithm().getAlgorithm(), SecurityProviderUtility.getBCProvider());
            sign.initVerify(publicKey);
            sign.update(sigInBytes);

            return sign.verify(sigBytes);
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }
}
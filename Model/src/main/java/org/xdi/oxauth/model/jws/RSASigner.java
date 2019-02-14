/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jws;

import org.xdi.oxauth.model.crypto.Certificate;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import java.security.*;
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

    public RSASigner(SignatureAlgorithm signatureAlgorithm, Certificate certificate) {
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

            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

            Signature signature = Signature.getInstance(getSignatureAlgorithm().getAlgorithm(), "BC");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(Util.UTF8_STRING_ENCODING));

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
            byte[] sigInBytes = signingInput.getBytes(Util.UTF8_STRING_ENCODING);

            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(
                    rsaPublicKey.getModulus(),
                    rsaPublicKey.getPublicExponent());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

            Signature sign = Signature.getInstance(getSignatureAlgorithm().getAlgorithm(), "BC");
            sign.initVerify(publicKey);
            sign.update(sigInBytes);

            return sign.verify(sigBytes);
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }
}
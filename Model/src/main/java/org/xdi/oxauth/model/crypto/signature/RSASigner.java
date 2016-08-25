/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.crypto.signature;

import com.google.common.base.Strings;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class RSASigner extends AbstractSigner {

    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;

    public RSASigner(SignatureAlgorithm signatureAlgorithm, RSAPrivateKey rsaPrivateKey) throws Exception {
        this(signatureAlgorithm);

        if (rsaPrivateKey == null) {
            throw new Exception("Invalid RSA private key");
        }

        this.rsaPrivateKey = rsaPrivateKey;
    }

    public RSASigner(SignatureAlgorithm signatureAlgorithm, RSAPublicKey rsaPublicKey) throws Exception {
        this(signatureAlgorithm);

        if (rsaPublicKey == null) {
            throw new Exception("Invalid RSA public key");
        }

        this.rsaPublicKey = rsaPublicKey;
    }

    private RSASigner(SignatureAlgorithm signatureAlgorithm) throws Exception {
        super(signatureAlgorithm);

        if (signatureAlgorithm == null || !SignatureAlgorithmFamily.RSA.equals(signatureAlgorithm.getFamily())) {
            throw new Exception("Invalid signature algorithm");
        }
    }

    @Override
    public String sign(String signingInput) throws Exception {
        if (Strings.isNullOrEmpty(signingInput)) {
            throw new Exception("Invalid signing input");
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
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("There was a problem in RSA signing", e);
        } catch (UnsupportedEncodingException e) {
            throw new Exception("There was a problem in RSA signing", e);
        } catch (SignatureException e) {
            throw new Exception("There was a problem in RSA signing", e);
        } catch (NoSuchProviderException e) {
            throw new Exception("There was a problem in RSA signing", e);
        } catch (InvalidKeyException e) {
            throw new Exception("There was a problem in RSA signing", e);
        } catch (InvalidKeySpecException e) {
            throw new Exception("There was a problem in RSA signing", e);
        }
    }

    @Override
    public boolean verifySignature(String signingInput, String signature) throws Exception {
        if (Strings.isNullOrEmpty(signingInput)) {
            return false;
        }
        if (Strings.isNullOrEmpty(signature)) {
            return false;
        }

        try {
            byte[] signatureBytes = Base64Util.base64urldecode(signature);
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(
                    rsaPublicKey.getModulus(),
                    rsaPublicKey.getPublicExponent());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

            Signature verifier = Signature.getInstance(getSignatureAlgorithm().getAlgorithm(), "BC");
            verifier.initVerify(publicKey);
            verifier.update(signingInput.getBytes());
            return verifier.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("There was a problem in RSA verifier", e);
        } catch (SignatureException e) {
            throw new Exception("There was a problem in RSA verifier", e);
        } catch (NoSuchProviderException e) {
            throw new Exception("There was a problem in RSA verifier", e);
        } catch (InvalidKeyException e) {
            throw new Exception("There was a problem in RSA verifier", e);
        } catch (InvalidKeySpecException e) {
            throw new Exception("There was a problem in RSA verifier", e);
        }
    }
}

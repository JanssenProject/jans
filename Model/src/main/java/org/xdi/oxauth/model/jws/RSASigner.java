/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jws;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.xdi.oxauth.model.crypto.Certificate;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
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
        } catch (InvalidKeySpecException e) {
            throw new SignatureException(e);
        } catch (InvalidKeyException e) {
            throw new SignatureException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e);
        } catch (NoSuchProviderException e) {
            throw new SignatureException(e);
        } catch (SignatureException e) {
            throw new SignatureException(e);
        } catch (UnsupportedEncodingException e) {
            throw new SignatureException(e);
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

        String algorithm = null;
        switch (getSignatureAlgorithm()) {
            case RS256:
                algorithm = "SHA-256";
                break;
            case RS384:
                algorithm = "SHA-384";
                break;
            case RS512:
                algorithm = "SHA-512";
                break;
            default:
                throw new SignatureException("Unsupported signature algorithm");
        }

        ASN1InputStream aIn = null;
        try {
            byte[] sigBytes = Base64Util.base64urldecode(signature);
            byte[] sigInBytes = signingInput.getBytes(Util.UTF8_STRING_ENCODING);

            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(
                    rsaPublicKey.getModulus(),
                    rsaPublicKey.getPublicExponent());

            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
            PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);

            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);

            byte[] decSig = cipher.doFinal(sigBytes);
            aIn = new ASN1InputStream(decSig);

            ASN1Sequence seq = (ASN1Sequence) aIn.readObject();

            MessageDigest hash = MessageDigest.getInstance(algorithm, "BC");
            hash.update(sigInBytes);

            ASN1OctetString sigHash = (ASN1OctetString) seq.getObjectAt(1);
            return MessageDigest.isEqual(hash.digest(), sigHash.getOctets());
        } catch (IOException e) {
            throw new SignatureException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e);
        } catch (InvalidKeyException e) {
            throw new SignatureException(e);
        } catch (InvalidKeySpecException e) {
            throw new SignatureException(e);
        } catch (NoSuchPaddingException e) {
            throw new SignatureException(e);
        } catch (BadPaddingException e) {
            throw new SignatureException(e);
        } catch (NoSuchProviderException e) {
            throw new SignatureException(e);
        } catch (IllegalBlockSizeException e) {
            throw new SignatureException(e);
        } catch (Exception e) {
            throw new SignatureException(e);
        } finally {
            IOUtils.closeQuietly(aIn);
        }
    }
}
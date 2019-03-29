/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jws;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.xdi.oxauth.model.crypto.Certificate;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class ECDSASigner extends AbstractJwsSigner {

    private ECDSAPrivateKey ecdsaPrivateKey;
    private ECDSAPublicKey ecdsaPublicKey;

    public ECDSASigner(SignatureAlgorithm signatureAlgorithm, ECDSAPrivateKey ecdsaPrivateKey) {
        super(signatureAlgorithm);
        this.ecdsaPrivateKey = ecdsaPrivateKey;
    }

    public ECDSASigner(SignatureAlgorithm signatureAlgorithm, ECDSAPublicKey ecdsaPublicKey) {
        super(signatureAlgorithm);
        this.ecdsaPublicKey = ecdsaPublicKey;
    }

    public ECDSASigner(SignatureAlgorithm signatureAlgorithm, Certificate certificate) {
        super(signatureAlgorithm);
        this.ecdsaPublicKey = certificate.getEcdsaPublicKey();
    }

    @Override
    public String generateSignature(String signingInput) throws SignatureException {
        if (getSignatureAlgorithm() == null) {
            throw new SignatureException("The signature algorithm is null");
        }
        if (ecdsaPrivateKey == null) {
            throw new SignatureException("The ECDSA private key is null");
        }
        if (signingInput == null) {
            throw new SignatureException("The signing input is null");
        }

        try {
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(getSignatureAlgorithm().getCurve().getName());
            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(ecdsaPrivateKey.getD(), ecSpec);

            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

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
        if (ecdsaPublicKey == null) {
            throw new SignatureException("The ECDSA public key is null");
        }
        if (signingInput == null) {
            throw new SignatureException("The signing input is null");
        }

        String algorithm;
        String curve;
        switch (getSignatureAlgorithm()) {
            case ES256:
                algorithm = "SHA256WITHECDSA";
                curve = "P-256";
                break;
            case ES384:
                algorithm = "SHA384WITHECDSA";
                curve = "P-384";
                break;
            case ES512:
                algorithm = "SHA512WITHECDSA";
                curve = "P-521";
                break;
            default:
                throw new SignatureException("Unsupported signature algorithm");
        }

        try {
            byte[] sigBytes = Base64Util.base64urldecode(signature);
            byte[] sigInBytes = signingInput.getBytes(Util.UTF8_STRING_ENCODING);

            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(curve);
            BigInteger q = ((ECCurve.AbstractFp) ecSpec.getCurve()).getField().getCharacteristic();
            ECFieldElement xFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getX());
            ECFieldElement yFieldElement = new ECFieldElement.Fp(q, ecdsaPublicKey.getY());
            ECPoint pointQ = new ECPoint.Fp(ecSpec.getCurve(), xFieldElement, yFieldElement);
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            Signature sig = Signature.getInstance(algorithm, "BC");
            sig.initVerify(publicKey);
            sig.update(sigInBytes);
            return sig.verify(sigBytes);
        } catch (InvalidKeySpecException e) {
            throw new SignatureException(e);
        } catch (InvalidKeyException e) {
            throw new SignatureException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e);
        } catch (NoSuchProviderException e) {
            throw new SignatureException(e);
        } catch (UnsupportedEncodingException e) {
            throw new SignatureException(e);
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }
}
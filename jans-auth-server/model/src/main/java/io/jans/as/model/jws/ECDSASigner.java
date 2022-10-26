/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jws;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.impl.ECDSA;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.ECDSAPrivateKey;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.util.Base64Util;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Implementing the AbstractJwsSigner, that uses ECDSA for signing.
 * 
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public class ECDSASigner extends AbstractJwsSigner {

    private ECDSAPrivateKey ecdsaPrivateKey;
    private ECDSAPublicKey ecdsaPublicKey;

    /**
     * Constructor.
     * 
     * @param signatureAlgorithm signature algorithm.
     * @param ecdsaPrivateKey ecdsa private key.
     */
    public ECDSASigner(SignatureAlgorithm signatureAlgorithm, ECDSAPrivateKey ecdsaPrivateKey) {
        super(signatureAlgorithm);
        this.ecdsaPrivateKey = ecdsaPrivateKey;
    }

    /**
     * Constructor.
     *
     * @param signatureAlgorithm signature algorithm.
     * @param ecdsaPublicKey ecdsa public key.
     */
    public ECDSASigner(SignatureAlgorithm signatureAlgorithm, ECDSAPublicKey ecdsaPublicKey) {
        super(signatureAlgorithm);
        this.ecdsaPublicKey = ecdsaPublicKey;
    }

    /**
     * Constructor.
     * 
     * @param signatureAlgorithm signature algorithm.
     * @param certificate certificate (uses RSA, EcDSA, EdDSA).
     */
    public ECDSASigner(SignatureAlgorithm signatureAlgorithm, io.jans.as.model.crypto.Certificate certificate) {
        super(signatureAlgorithm);
        this.ecdsaPublicKey = certificate.getEcdsaPublicKey();
    }

    /**
     * Generating a signature, using URL safe based format.
     */
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

            Signature signer = Signature.getInstance(getSignatureAlgorithm().getAlgorithm(), "BC");
            signer.initSign(privateKey);
            signer.update(signingInput.getBytes(StandardCharsets.UTF_8));

            byte[] signature = signer.sign();
            if (AlgorithmFamily.EC.equals(getSignatureAlgorithm().getFamily())) {
                int signatureLenght = ECDSA.getSignatureByteArrayLength(JWSAlgorithm.parse(getSignatureAlgorithm().getName()));
                signature = ECDSA.transcodeSignatureToConcat(signature, signatureLenght);
            }

            return Base64Util.base64urlencode(signature);
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }

    /**
     * Validating a signature.
     */
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
        SignatureAlgorithm signatureAlgorithm = getSignatureAlgorithm();
        try {
            byte[] sigBytes = Base64Util.base64urldecode(signature);
            if (AlgorithmFamily.EC.equals(getSignatureAlgorithm().getFamily())) {
                sigBytes = ECDSA.transcodeSignatureToDER(sigBytes);
            }
            byte[] sigInBytes = signingInput.getBytes(StandardCharsets.UTF_8);

            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(signatureAlgorithm.getCurve().getAlias());
            ECPoint pointQ = ecSpec.getCurve().createPoint(ecdsaPublicKey.getX(), ecdsaPublicKey.getY());

            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointQ, ecSpec);

            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            Signature sig = Signature.getInstance(signatureAlgorithm.getAlgorithm(), "BC");
            sig.initVerify(publicKey);
            sig.update(sigInBytes);
            return sig.verify(sigBytes);
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }
}
/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.crypto.signature;

import io.jans.as.model.crypto.signature.EllipticEdvardsCurve;
import io.jans.as.model.exception.SignatureException;
import io.jans.util.security.SecurityProviderUtility;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;

import jakarta.inject.Named;

import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

@Named
public class SHA256withECDSASignatureVerification implements SignatureVerification {

    @Override
    public boolean checkSignature(X509Certificate certificate, byte[] signedBytes, byte[] signature) throws SignatureException {
        return checkSignature(certificate.getPublicKey(), signedBytes, signature);
    }

    @Override
    public boolean checkSignature(PublicKey publicKey, byte[] signedBytes, byte[] signature) throws SignatureException {
        boolean isValid = false;
        try {
            Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA", SecurityProviderUtility.getBCProvider());
            ecdsaSignature.initVerify(publicKey);
            ecdsaSignature.update(signedBytes);

            isValid = ecdsaSignature.verify(signature);
        } catch (GeneralSecurityException ex) {
            throw new SignatureException(ex);
        }

        return isValid;
    }

    @Override
    public PublicKey decodePublicKey(byte[] encodedPublicKey) throws SignatureException {
        String ecAlias = "secp256r1";
        EllipticEdvardsCurve ecCurve = EllipticEdvardsCurve.fromString(ecAlias);
        X9ECParameters curve = SECNamedCurves.getByName(ecAlias);
        org.bouncycastle.math.ec.ECPoint ecPoint = curve.getCurve().decodePoint(encodedPublicKey);
        ECPoint point = new ECPoint(ecPoint.getXCoord().toBigInteger(), ecPoint.getYCoord().toBigInteger());
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", SecurityProviderUtility.getBCProvider());
            parameters.init(new ECGenParameterSpec(ecCurve.getName()));
            ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
            KeySpec publicKeySpec = new ECPublicKeySpec(point, ecParameters);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", SecurityProviderUtility.getBCProvider());
            return keyFactory.generatePublic(publicKeySpec);
        }
        catch (NoSuchAlgorithmException | InvalidParameterSpecException | InvalidKeySpecException e) {
            throw new SignatureException(e);
        }
    }

    @Override
    public byte[] hash(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] hash(String str) {
        return hash(str.getBytes());
    }
}

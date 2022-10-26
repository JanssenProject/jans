/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.crypto.signature;

import io.jans.as.model.exception.SignatureException;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import jakarta.inject.Named;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

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
            Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA", "BC");
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
        X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
        ECPoint point = curve.getCurve().decodePoint(encodedPublicKey);

        try {
            return KeyFactory.getInstance("ECDSA").generatePublic(
                    new ECPublicKeySpec(point,
                            new ECParameterSpec(
                                    curve.getCurve(),
                                    curve.getG(),
                                    curve.getN(),
                                    curve.getH()
                            )
                    )
            );
        } catch (GeneralSecurityException ex) {
            throw new SignatureException(ex);
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

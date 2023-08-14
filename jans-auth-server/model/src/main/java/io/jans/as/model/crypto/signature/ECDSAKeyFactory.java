/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.KeyFactory;
import io.jans.util.security.SecurityProviderUtility;

import org.apache.commons.lang.StringUtils;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.cert.CertIOException;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;


/**
 * Factory to create asymmetric Public and Private Keys for the Elliptic Curve
 * Digital Signature Algorithm (ECDSA)
 *
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public class ECDSAKeyFactory extends KeyFactory<ECDSAPrivateKey, ECDSAPublicKey> {

    private final ECDSAPrivateKey ecdsaPrivateKey;
    private final ECDSAPublicKey ecdsaPublicKey;

    public ECDSAKeyFactory(SignatureAlgorithm signatureAlgorithm, String dnName) throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidAlgorithmParameterException, OperatorCreationException, CertificateException, CertIOException {
        if (signatureAlgorithm == null) {
            throw new InvalidParameterException("The signature algorithm cannot be null");
        }

        this.signatureAlgorithm = signatureAlgorithm;

        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", SecurityProviderUtility.getBCProvider());

        parameters.init(new ECGenParameterSpec(signatureAlgorithm.getCurve().getName()));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", SecurityProviderUtility.getBCProvider());
        keyGen.initialize(ecParameters, new SecureRandom());

        keyPair = keyGen.generateKeyPair();

        ECPrivateKey privateKeySpec = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKeySpec = (ECPublicKey) keyPair.getPublic();

        BigInteger x = publicKeySpec.getW().getAffineX();
        BigInteger y = publicKeySpec.getW().getAffineY();
        BigInteger s = privateKeySpec.getS();

        this.ecdsaPrivateKey = new ECDSAPrivateKey(signatureAlgorithm, s);
        this.ecdsaPublicKey = new ECDSAPublicKey(signatureAlgorithm, x, y);

        if (StringUtils.isNotBlank(dnName)) {
            // Create certificate
            GregorianCalendar startDate = new GregorianCalendar(); // time from which certificate is valid
            GregorianCalendar expiryDate = new GregorianCalendar(); // time after which certificate is not valid
            expiryDate.add(Calendar.YEAR, 1);

            this.certificate = generateV3Certificate(startDate.getTime(), expiryDate.getTime(), dnName);
        }
    }

    @Override
    public ECDSAPrivateKey getPrivateKey() {
        return ecdsaPrivateKey;
    }

    @Override
    public ECDSAPublicKey getPublicKey() {
        return ecdsaPublicKey;
    }

    @Override
    public Certificate getCertificate() {
        return certificate;
    }
}

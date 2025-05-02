/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;

import java.security.InvalidParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.KeyFactory;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.util.security.SecurityProviderUtility;

/**
 * Factory to create asymmetric Public and Private Keys for the RSA algorithm
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 *
 * @deprecated
 */
@SuppressWarnings("java:S1133") 
@Deprecated
public class RSAKeyFactory extends KeyFactory<RSAPrivateKey, RSAPublicKey> {

    public static final int DEF_KEYLENGTH = 2048;

    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;

    /**
     * @param signatureAlgorithm
     * @param dnName
     * @throws NoSuchAlgorithmException
     * @throws OperatorCreationException
     * @throws CertificateException
     * @throws CertIOException
     * @throws SignatureException 
     *
     * @deprecated
     */
    @Deprecated
    public RSAKeyFactory(SignatureAlgorithm signatureAlgorithm, String dnName) throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, CertIOException, SignatureException {
        if (signatureAlgorithm == null) {
            throw new InvalidParameterException("The signature algorithm cannot be null");
        }

        this.signatureAlgorithm = signatureAlgorithm;

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", SecurityProviderUtility.getBCProvider());
        keyGen.initialize(2048, new SecureRandom());

        keyPair = keyGen.generateKeyPair();

        java.security.interfaces.RSAPrivateKey jcersaPrivateCrtKey = (java.security.interfaces.RSAPrivateKey) keyPair.getPrivate();
        java.security.interfaces.RSAPublicKey jcersaPublicKey = (java.security.interfaces.RSAPublicKey) keyPair.getPublic();

        rsaPrivateKey = new RSAPrivateKey(signatureAlgorithm, jcersaPrivateCrtKey.getModulus(), jcersaPrivateCrtKey.getPrivateExponent());

        rsaPublicKey = new RSAPublicKey(jcersaPublicKey.getModulus(), jcersaPublicKey.getPublicExponent());

        if (StringUtils.isNotBlank(dnName)) {
            // Create certificate
            GregorianCalendar startDate = new GregorianCalendar(); // time from which certificate is valid
            GregorianCalendar expiryDate = new GregorianCalendar(); // time after which certificate is not valid
            expiryDate.add(Calendar.YEAR, 1);

            this.certificate = generateV3Certificate(startDate.getTime(), expiryDate.getTime(), dnName);
        }
    }

    /**
     *
     * @param pKey
     *
     * @deprecated
     */
    @Deprecated
    public RSAKeyFactory(JSONWebKey pKey) {
        if (pKey == null) {
            throw new IllegalArgumentException("Key value must not be null.");
        }

        rsaPrivateKey = new RSAPrivateKey(
                null,
                pKey.getN(),
                pKey.getE());
        rsaPublicKey = new RSAPublicKey(
                pKey.getN(),
                pKey.getE());
        certificate = null;
    }

    public static RSAKeyFactory valueOf(JSONWebKey pKey) {
        return new RSAKeyFactory(pKey);
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return rsaPrivateKey;
    }

    @Override
    public RSAPublicKey getPublicKey() {
        return rsaPublicKey;
    }

    @Override
    public Certificate getCertificate() {
        return certificate;
    }
}
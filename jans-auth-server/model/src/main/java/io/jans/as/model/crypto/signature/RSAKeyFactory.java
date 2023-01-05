/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import io.jans.as.model.crypto.Certificate;
import io.jans.as.model.crypto.KeyFactory;
import io.jans.as.model.jwk.JSONWebKey;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.x509.X509V1CertificateGenerator;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Factory to create asymmetric Public and Private Keys for the RSA algorithm
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 * @deprecated Used for Tests
 */
@Deprecated
public class RSAKeyFactory extends KeyFactory<RSAPrivateKey, RSAPublicKey> {

    public static final int DEF_KEYLENGTH = 2048;

    private final RSAPrivateKey rsaPrivateKey;
    private final RSAPublicKey rsaPublicKey;
    private Certificate certificate;

    @Deprecated
    public RSAKeyFactory(SignatureAlgorithm signatureAlgorithm, String dnName)
            throws InvalidParameterException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException,
            InvalidKeyException, CertificateEncodingException {
        if (signatureAlgorithm == null) {
            throw new InvalidParameterException("The signature algorithm cannot be null");
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(RSAKeyFactory.DEF_KEYLENGTH, new SecureRandom());

        KeyPair keyPair = keyGen.generateKeyPair();

        BCRSAPrivateCrtKey jcersaPrivateCrtKey = (BCRSAPrivateCrtKey) keyPair.getPrivate();
        BCRSAPublicKey jcersaPublicKey = (BCRSAPublicKey) keyPair.getPublic();

        rsaPrivateKey = new RSAPrivateKey(signatureAlgorithm, jcersaPrivateCrtKey.getModulus(), jcersaPrivateCrtKey.getPrivateExponent());

        rsaPublicKey = new RSAPublicKey(jcersaPublicKey.getModulus(), jcersaPublicKey.getPublicExponent());

        if (StringUtils.isNotBlank(dnName)) {
            // Create certificate
            GregorianCalendar startDate = new GregorianCalendar(); // time from which certificate is valid
            GregorianCalendar expiryDate = new GregorianCalendar(); // time after which certificate is not valid
            expiryDate.add(Calendar.YEAR, 1);
            BigInteger serialNumber = new BigInteger(1024, new SecureRandom()); // serial number for certificate

            X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
            X500Principal principal = new X500Principal(dnName);

            certGen.setSerialNumber(serialNumber);
            certGen.setIssuerDN(principal);
            certGen.setNotBefore(startDate.getTime());
            certGen.setNotAfter(expiryDate.getTime());
            certGen.setSubjectDN(principal); // note: same as issuer
            certGen.setPublicKey(keyPair.getPublic());
            certGen.setSignatureAlgorithm(signatureAlgorithm.getAlgorithm());

            X509Certificate x509Certificate = certGen.generate(jcersaPrivateCrtKey, "BC");
            certificate = new Certificate(signatureAlgorithm, x509Certificate);
        }
    }

    @Deprecated
    public RSAKeyFactory(JSONWebKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Key value must not be null.");
        }

        rsaPrivateKey = new RSAPrivateKey(
                null,
                key.getN(),
                key.getE());
        rsaPublicKey = new RSAPublicKey(
                key.getN(),
                key.getE());
        certificate = null;
    }

    public static RSAKeyFactory valueOf(JSONWebKey key) {
        return new RSAKeyFactory(key);
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

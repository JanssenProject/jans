/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto.signature;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
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
 */
@Deprecated
public class RSAKeyFactory extends KeyFactory<RSAPrivateKey, RSAPublicKey> {

    public static final int DEF_KEYLENGTH = 2048;

    private SignatureAlgorithm signatureAlgorithm;
    private KeyPair keyPair;

    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;
    private Certificate certificate;

    @Deprecated
    public RSAKeyFactory(SignatureAlgorithm signatureAlgorithm, String dnName) throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, CertIOException {
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

    public Certificate generateV3Certificate(Date startDate, Date expirationDate, String dnName) throws OperatorCreationException, CertificateException, CertIOException {
        BigInteger serialNumber = new BigInteger(1024, new SecureRandom()); // serial number for certificate
        X500Name name = new X500Name(dnName);

        JcaX509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(name, serialNumber, startDate, expirationDate, name, keyPair.getPublic());

        ASN1EncodableVector purposes = new ASN1EncodableVector();
        purposes.add(KeyPurposeId.id_kp_serverAuth);
        purposes.add(KeyPurposeId.id_kp_clientAuth);
        purposes.add(KeyPurposeId.anyExtendedKeyUsage);

        ASN1ObjectIdentifier extendedKeyUsage = new ASN1ObjectIdentifier("2.5.29.37").intern();
        certGen.addExtension(extendedKeyUsage, false, new DERSequence(purposes));

        X509CertificateHolder certHolder = certGen.build(new JcaContentSignerBuilder(signatureAlgorithm.getAlgorithm()).setProvider(SecurityProviderUtility.getBCProviderName()).build(keyPair.getPrivate()));
        X509Certificate x509Certificate = new JcaX509CertificateConverter().setProvider(SecurityProviderUtility.getBCProviderName()).getCertificate(certHolder);

        return new Certificate(signatureAlgorithm, x509Certificate);
    }

    @Deprecated
    public RSAKeyFactory(JSONWebKey p_key) {
        if (p_key == null) {
            throw new IllegalArgumentException("Key value must not be null.");
        }

        rsaPrivateKey = new RSAPrivateKey(
                null,
                p_key.getN(),
                p_key.getE());
        rsaPublicKey = new RSAPublicKey(
                p_key.getN(),
                p_key.getE());
        certificate = null;
    }

    public static RSAKeyFactory valueOf(JSONWebKey p_key) {
        return new RSAKeyFactory(p_key);
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
/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

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

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.util.security.SecurityProviderUtility;

/**
 * Factory to create asymmetric Public and Private Keys
 *
 * @author Javier Rojas Blum Date: 10.22.2012
 */
public abstract class KeyFactory<E extends PrivateKey, F extends PublicKey> {

    protected SignatureAlgorithm signatureAlgorithm;
    protected KeyPair keyPair;
    protected Certificate certificate;

    public abstract E getPrivateKey();

    public abstract F getPublicKey();

    public abstract Certificate getCertificate();

    public Key<E, F> getKey() {
        Key<E, F> key = new Key<>();

        key.setPrivateKey(getPrivateKey());
        key.setPublicKey(getPublicKey());
        key.setCertificate(getCertificate());

        return key;
    }

    public Certificate generateV3Certificate(Date startDate, Date expirationDate, String dnName) throws OperatorCreationException, CertificateException, CertIOException, SignatureException {
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
}
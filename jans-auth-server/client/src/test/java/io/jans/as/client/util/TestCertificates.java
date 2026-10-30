/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.as.client.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Minimal in-memory X.509 certificate generation for loopback TLS tests.
 * Uses BouncyCastle (already a test/runtime dependency of this module).
 * Not for production use.
 */
final class TestCertificates {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TestCertificates() {
    }

    /** Create a self-signed CA certificate. */
    static X509Certificate selfSignedCa(KeyPair keyPair, String cn, Date notBefore, Date notAfter) throws Exception {
        X500Name name = caName(cn);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name,
                new BigInteger(160, RANDOM),
                notBefore,
                notAfter,
                name,
                keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign | KeyUsage.digitalSignature));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /** Create a server (end-entity) certificate signed by the given CA. */
    static X509Certificate serverCert(KeyPair serverPair, KeyPair caPair, X509Certificate caCert,
                                      String cn, String[] dnsSans, Date notBefore, Date notAfter) throws Exception {
        // Build the issuer name with the SAME RDN ordering as the CA subject so that
        // issuer/subject DN comparison matches during PKIX path building.
        X500Name issuer = caName(caCn(caCert));
        X500Name subject = new X500Name("CN=" + cn + ",O=JansRegressionLab");
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                new BigInteger(160, RANDOM),
                notBefore,
                notAfter,
                subject,
                serverPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        GeneralName[] names = new GeneralName[dnsSans.length];
        for (int i = 0; i < dnsSans.length; i++) {
            names[i] = new GeneralName(GeneralName.dNSName, dnsSans[i]);
        }
        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(caPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    /** Canonical CA name: CN first, then O (matches the subject used for self-signed CA). */
    private static X500Name caName(String cn) {
        return new X500Name("CN=" + cn + ",O=JansRegressionLab");
    }

    /** Extract the CN value from a certificate subject (defaults to JansTestCA). */
    private static String caCn(X509Certificate caCert) {
        String dn = caCert.getSubjectX500Principal().getName();
        for (String part : dn.split(",")) {
            String p = part.trim();
            if (p.startsWith("CN=")) {
                return p.substring(3);
            }
        }
        return "JansTestCA";
    }
}

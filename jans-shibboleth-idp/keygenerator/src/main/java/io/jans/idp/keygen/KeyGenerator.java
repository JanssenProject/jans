package io.jans.idp.keygen;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.util.Date;
import java.security.SecureRandom;

import javax.security.auth.x500.X500Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(KeyGenerator.class);
    
    private static final String DEFAULT_ALGORITHM = "RSA";
    private static final int DEFAULT_KEY_SIZE = 3072;
    private static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final long DEFAULT_VALIDITY_YEARS = 10;

    public static void main(String[] args) {
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }

        String keystorePath = args[0];
        String keystorePassword = args[1];
        String keyAlias = args[2];
        String keyPassword = args.length > 3 ? args[3] : keystorePassword;
        String subjectDn = args.length > 4 ? args[4] : "CN=Janssen Shibboleth IDP";
        int keySize = args.length > 5 ? Integer.parseInt(args[5]) : DEFAULT_KEY_SIZE;

        try {
            generateSamlSigningKey(keystorePath, keystorePassword, keyAlias, keyPassword, subjectDn, keySize);
            LOG.info("Successfully generated SAML signing key in {}", keystorePath);
        } catch (Exception e) {
            LOG.error("Failed to generate key", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: KeyGenerator <keystore-path> <keystore-password> <key-alias> " +
                "[key-password] [subject-dn] [key-size]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  keystore-path     - Path to output keystore file");
        System.out.println("  keystore-password - Password for the keystore");
        System.out.println("  key-alias         - Alias for the generated key");
        System.out.println("  key-password      - Password for the key (default: same as keystore)");
        System.out.println("  subject-dn        - Subject DN for the certificate (default: CN=Janssen Shibboleth IDP)");
        System.out.println("  key-size          - RSA key size in bits (default: 3072)");
    }

    public static void generateSamlSigningKey(String keystorePath, String keystorePassword,
                                               String keyAlias, String keyPassword,
                                               String subjectDn, int keySize) throws Exception {
        
        LOG.info("Generating RSA key pair with {} bits", keySize);
        
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);
        keyGen.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        
        X509Certificate certificate = generateSelfSignedCertificate(keyPair, subjectDn);
        
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        
        Certificate[] certificateChain = new Certificate[] { certificate };
        keyStore.setKeyEntry(keyAlias, keyPair.getPrivate(), keyPassword.toCharArray(), certificateChain);
        
        try (OutputStream os = new FileOutputStream(keystorePath)) {
            keyStore.store(os, keystorePassword.toCharArray());
        }
        
        LOG.info("Keystore saved to {}", keystorePath);
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String subjectDn) 
            throws Exception {
        
        X500Principal subject = new X500Principal(subjectDn);
        
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + DEFAULT_VALIDITY_YEARS * 365L * 24 * 60 * 60 * 1000);
        
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        
        org.bouncycastle.asn1.x500.X500Name issuerName = new org.bouncycastle.asn1.x500.X500Name(subjectDn);
        org.bouncycastle.asn1.x500.X500Name subjectName = new org.bouncycastle.asn1.x500.X500Name(subjectDn);
        
        org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder certBuilder = 
            new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                issuerName,
                serialNumber,
                notBefore,
                notAfter,
                subjectName,
                keyPair.getPublic()
            );
        
        org.bouncycastle.operator.ContentSigner signer = 
            new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(DEFAULT_SIGNATURE_ALGORITHM)
                .build(keyPair.getPrivate());
        
        org.bouncycastle.cert.X509CertificateHolder certHolder = certBuilder.build(signer);
        
        return new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
            .getCertificate(certHolder);
    }

    public static KeyPair generateKeyPair(int keySize) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(DEFAULT_ALGORITHM);
        keyGen.initialize(keySize, new SecureRandom());
        return keyGen.generateKeyPair();
    }
}

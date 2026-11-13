/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.util.security.SecurityProviderUtility;

class TraceKeyLoaderTest {

    private static final char[] PASSWORD = "keystore-pass".toCharArray();

    @TempDir
    Path dir;

    @BeforeAll
    static void installProvider() {
        SecurityProviderUtility.installBCProvider(true);
    }

    private Path keystore(String alias, KeyPair keys) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(TraceKeyLoader.KEYSTORE_TYPE, SecurityProviderUtility.getBCProvider());
        keyStore.load(null, null);
        // BCFKS, like PKCS12, requires a certificate chain with a private key
        X500Name name = new X500Name("CN=trace-producer");
        Date now = new Date();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE,
                new Date(now.getTime() - 60_000L), new Date(now.getTime() + 86_400_000L), name, keys.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("Ed25519")
                .setProvider(SecurityProviderUtility.getBCProvider()).build(keys.getPrivate());
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(SecurityProviderUtility.getBCProvider()).getCertificate(builder.build(signer));
        keyStore.setKeyEntry(alias, keys.getPrivate(), PASSWORD, new Certificate[] {cert});
        Path file = dir.resolve("trace.bcfks");
        try (OutputStream out = Files.newOutputStream(file)) {
            keyStore.store(out, PASSWORD);
        }
        return file;
    }

    private static KeyPair ed25519() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519", SecurityProviderUtility.getBCProvider()).generateKeyPair();
    }

    @Test
    void loadsEd25519KeyByAlias() throws Exception {
        KeyPair keys = ed25519();
        Path file = keystore("trace-producer", keys);

        PrivateKey loaded = TraceKeyLoader.load(file.toString(), PASSWORD, "trace-producer");

        // A signature made with the loaded key verifies with the original public key
        Signature signer = Signature.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        signer.initSign(loaded);
        signer.update(new byte[] {1, 2, 3});
        byte[] signature = signer.sign();
        Signature verifier = Signature.getInstance("Ed25519", SecurityProviderUtility.getBCProvider());
        verifier.initVerify(keys.getPublic());
        verifier.update(new byte[] {1, 2, 3});
        assertTrue(verifier.verify(signature));
    }

    @Test
    void wrongPasswordFails() throws Exception {
        String path = keystore("trace-producer", ed25519()).toString();
        char[] wrong = "wrong".toCharArray();
        assertThrows(IllegalStateException.class, () -> TraceKeyLoader.load(path, wrong, "trace-producer"));
    }

    @Test
    void missingAliasFails() throws Exception {
        String path = keystore("trace-producer", ed25519()).toString();
        assertThrows(IllegalStateException.class, () -> TraceKeyLoader.load(path, PASSWORD, "no-such-alias"));
    }

    @Test
    void configurationNamesTheKeyAndDecryptsThePassword() throws Exception {
        Path file = keystore("trace-producer", ed25519());
        Fido2Configuration conf = new Fido2Configuration();
        conf.setFido2TraceKeyStorePath(file.toString());
        conf.setFido2TraceKeyAlias("trace-producer");
        conf.setFido2TraceKeyStorePassword("encrypted-form");

        PrivateKey key = TraceKeyLoader.fromConfiguration(conf, stored -> {
            assertEquals("encrypted-form", stored);
            return "keystore-pass";
        });

        assertEquals("Ed25519", key.getAlgorithm());
    }

    @Test
    void configurationWithoutKeyFails() {
        Fido2Configuration empty = new Fido2Configuration();
        assertThrows(IllegalStateException.class, () -> TraceKeyLoader.fromConfiguration(empty, value -> value));
    }
}

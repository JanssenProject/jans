/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.client.util;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * Unit tests for {@link ClientUtil#createHttpClient(String, String[])}.
 *
 * <p>These tests exercise the real factory method against loopback HTTPS servers using
 * certificates generated in-memory. They never contact external or production systems.
 * They cover two regressions that previously existed in the method:</p>
 * <ul>
 *   <li>The pooling connection manager was closed inside the factory (try-with-resources),
 *       so the returned client could never execute a request ("Connection pool shut down").</li>
 *   <li>Hostname verification was disabled (NoopHostnameVerifier), so a certificate issued
 *       for a different host was accepted.</li>
 * </ul>
 *
 * <p>The factory builds its client via {@code SSLContexts.createDefault()}, which reads the
 * default truststore from the {@code javax.net.ssl.trustStore} system properties. These tests
 * write a temporary truststore containing only the in-memory test CA and point that property
 * at it for the duration of the suite, restoring the original values afterwards.</p>
 */
public class ClientUtilHttpClientTlsTest {

    private static final String TLS_VERSION = "TLSv1.2";
    private static final String[] CIPHERS = {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"};

    private String originalTrustStore;
    private String originalTrustStorePassword;
    private String originalTrustStoreType;
    private Path trustStoreFile;

    private HttpsServer goodServer;       // presents cert for "localhost"
    private HttpsServer wrongHostServer;  // presents cert for "wronghost.example"
    private HttpsServer untrustedServer;  // self-signed, not under our CA
    private String goodUrl;
    private String wrongHostUrl;
    private String untrustedUrl;

    @BeforeClass
    public void setUp() throws Exception {
        // Capture the JVM's pre-test truststore settings first, so @AfterClass can restore them
        // even if certificate generation, server startup or truststore creation fails below.
        originalTrustStore = System.getProperty("javax.net.ssl.trustStore");
        originalTrustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        originalTrustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 60000);
        Date notAfter = new Date(now + 365L * 24 * 3600 * 1000);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);

        // Local CA
        KeyPair caPair = kpg.generateKeyPair();
        X509Certificate caCert = TestCertificates.selfSignedCa(caPair, "JansTestCA", notBefore, notAfter);

        // Server certs signed by the CA
        KeyPair goodPair = kpg.generateKeyPair();
        X509Certificate goodCert = TestCertificates.serverCert(goodPair, caPair, caCert,
                "localhost", new String[]{"localhost"}, notBefore, notAfter);
        KeyPair wrongPair = kpg.generateKeyPair();
        X509Certificate wrongCert = TestCertificates.serverCert(wrongPair, caPair, caCert,
                "wronghost.example", new String[]{"wronghost.example"}, notBefore, notAfter);

        // Untrusted: self-signed, NOT under our CA
        KeyPair untrustedPair = kpg.generateKeyPair();
        X509Certificate untrustedCert = TestCertificates.selfSignedCa(untrustedPair, "localhost", notBefore, notAfter);

        goodServer = startServer(goodPair.getPrivate(), goodCert, caCert);
        wrongHostServer = startServer(wrongPair.getPrivate(), wrongCert, caCert);
        untrustedServer = startServer(untrustedPair.getPrivate(), untrustedCert, null);

        goodUrl = "https://localhost:" + goodServer.getAddress().getPort() + "/";
        wrongHostUrl = "https://localhost:" + wrongHostServer.getAddress().getPort() + "/";
        untrustedUrl = "https://localhost:" + untrustedServer.getAddress().getPort() + "/";

        // Truststore containing ONLY our CA: good/wrong certs chain-trust, untrusted does not.
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("jans-test-ca", caCert);
        trustStoreFile = Files.createTempFile("jans-test-truststore", ".p12");
        try (OutputStream out = Files.newOutputStream(trustStoreFile)) {
            trustStore.store(out, "changeit".toCharArray());
        }

        System.setProperty("javax.net.ssl.trustStore", trustStoreFile.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (goodServer != null) goodServer.stop(0);
        if (wrongHostServer != null) wrongHostServer.stop(0);
        if (untrustedServer != null) untrustedServer.stop(0);
        restore("javax.net.ssl.trustStore", originalTrustStore);
        restore("javax.net.ssl.trustStorePassword", originalTrustStorePassword);
        restore("javax.net.ssl.trustStoreType", originalTrustStoreType);
        if (trustStoreFile != null) {
            try { Files.deleteIfExists(trustStoreFile); } catch (Exception ignored) {}
        }
    }

    /** The client returned by the factory must be able to execute a request (pool not closed). */
    @Test
    public void createHttpClient_returnedClientCanExecuteRequest() throws Exception {
        CloseableHttpClient client = ClientUtil.createHttpClient(TLS_VERSION, CIPHERS);
        assertNotNull(client, "createHttpClient returned null");
        try {
            HttpResponse response = client.execute(new HttpGet(goodUrl));
            assertEquals(response.getStatusLine().getStatusCode(), 200,
                    "Returned client failed to execute a request; the connection manager may be closed prematurely");
        } finally {
            client.close();
        }
    }

    /** A certificate issued for a different host must be rejected (hostname verification enabled). */
    @Test
    public void createHttpClient_hostnameMismatchRejected() throws Exception {
        CloseableHttpClient client = ClientUtil.createHttpClient(TLS_VERSION, CIPHERS);
        assertNotNull(client, "createHttpClient returned null");
        try {
            client.execute(new HttpGet(wrongHostUrl));
            fail("Expected SSLPeerUnverifiedException for hostname mismatch, but the request succeeded. " +
                    "Hostname verification appears to be disabled.");
        } catch (SSLPeerUnverifiedException expected) {
            // hostname verification correctly rejected the mismatched certificate
        } finally {
            client.close();
        }
    }

    /** A self-signed certificate not under the trusted CA must be rejected (chain validation intact). */
    @Test
    public void createHttpClient_untrustedCertificateRejected() throws Exception {
        CloseableHttpClient client = ClientUtil.createHttpClient(TLS_VERSION, CIPHERS);
        assertNotNull(client, "createHttpClient returned null");
        try {
            client.execute(new HttpGet(untrustedUrl));
            fail("Expected SSLHandshakeException for an untrusted certificate, but the request succeeded. " +
                    "Certificate-chain validation appears to be disabled.");
        } catch (SSLHandshakeException expected) {
            // chain validation correctly rejected the untrusted certificate
        } finally {
            client.close();
        }
    }

    private HttpsServer startServer(PrivateKey key, X509Certificate cert, X509Certificate chainCa) throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        Certificate[] chain = chainCa != null ? new Certificate[]{cert, chainCa} : new Certificate[]{cert};
        ks.setKeyEntry("server", key, "changeit".toCharArray(), chain);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "changeit".toCharArray());
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), null, null);
        server.setHttpsConfigurator(new HttpsConfigurator(ssl));
        server.createContext("/", exchange -> {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream o = exchange.getResponseBody()) {
                o.write(body);
            }
        });
        server.start();
        return server;
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}

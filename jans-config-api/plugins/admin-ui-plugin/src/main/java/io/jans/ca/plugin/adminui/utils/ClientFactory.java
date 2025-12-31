package io.jans.ca.plugin.adminui.utils;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.List;

public class ClientFactory {

    private static final ClientFactory INSTANCE = new ClientFactory();

    private ClientFactory() {}

    public static ClientFactory instance() {
        return INSTANCE;
    }

    public static Invocation.Builder getClientBuilder(String url) throws NoSuchAlgorithmException {

        /*
         * Prefer TLS 1.3, allow TLS 1.2 fallback
         * GH issue: https://github.com/JanssenProject/jans/issues/12484
         */
        SSLContext sslContext = SSLContext.getInstance("TLS");
        try {
            sslContext.init(null, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SSLContext", e);
        }

        SSLParameters sslParameters = sslContext.getDefaultSSLParameters();

        /*
         * Enable protocols: TLS 1.3 first, TLS 1.2 fallback
         */
        sslParameters.setProtocols(new String[] {
                "TLSv1.3",
                "TLSv1.2"
        });

        /*
         * TLS 1.2 cipher suite allow-list only
         * (TLS 1.3 ciphers are not configurable in JSSE)
         */
        sslParameters.setCipherSuites(new String[] {
                // ECDHE + RSA
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",

                // ECDHE + ECDSA
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256"
        });

        /*
         * Named groups (curves + strong FFDHE only)
         */
        /*sslParameters.setNamedGroups(new String[] {
                "x25519",
                "secp256r1",
                "secp384r1",
                "secp521r1",
                "ffdhe3072",
                "ffdhe4096"
        });*/

        /*
         * Signature algorithms allow-list (no SHA-1)
         */
        /*sslParameters.setSignatureSchemes(new String[] {
                "rsa_pss_rsae_sha256",
                "rsa_pss_rsae_sha384",
                "ecdsa_secp256r1_sha256",
                "ecdsa_secp384r1_sha384",
                "rsa_pkcs1_sha256",
                "rsa_pkcs1_sha384"
        });*/

        /*
         * Apply SSLParameters globally to this client
         */
        return ClientBuilder.newBuilder()
                .sslContext(sslContext)
                .build()
                .target(url)
                .request();
    }
}

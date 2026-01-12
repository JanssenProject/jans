package io.jans.ca.plugin.adminui.utils;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class ClientFactory {

    private static final ClientFactory INSTANCE = new ClientFactory();

    /**
 * Prevents external instantiation to enforce the singleton pattern for this class.
 */
private ClientFactory() {}

    /**
     * Obtain the application's single shared ClientFactory.
     *
     * @return the shared ClientFactory instance
     */
    public static ClientFactory instance() {
        return INSTANCE;
    }

    /**
     * Create an Invocation.Builder backed by a JAX-RS client configured with a custom TLS setup for the given URL.
     *
     * <p>The client is configured to prefer TLS 1.3 with TLS 1.2 as a fallback and restricts TLS 1.2 cipher suites
     * to a predefined allow-list.</p>
     *
     * @param url the target endpoint URL for the returned Invocation.Builder
     * @return an Invocation.Builder targeting the provided URL, using a client configured with the described TLS settings
     * @throws NoSuchAlgorithmException if the TLS SSLContext algorithm is not available
     * @throws KeyManagementException if the SSLContext cannot be initialized
     */
    public Invocation.Builder getClientBuilder(String url) throws NoSuchAlgorithmException, KeyManagementException {

        /*
         * Prefer TLS 1.3, allow TLS 1.2 fallback
         * GH issue: https://github.com/JanssenProject/jans/issues/12484
         */
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
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
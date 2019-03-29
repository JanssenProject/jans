/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.net;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.gluu.util.StringHelper;

/**
 * HTTP client with SSL support
 *
 * @author Yuriy Movchan
 * @version 1.0, 05/27/2013
 */
public class SslDefaultHttpClient extends DefaultHttpClient {

    private String trustStoreType, keyStoreType;
    private String trustStorePath, keyStorePath;
    private String trustStorePassword, keyStorePassword;

    private TrustManager[] trustManagers;

    private boolean useTrustManager = false;
    private boolean useKeyManager = false;

    public SslDefaultHttpClient() {
    }

    public SslDefaultHttpClient(TrustManager trustManager) {
        this.trustManagers = new TrustManager[] {trustManager};
    }

    public SslDefaultHttpClient(TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
    }

    public SslDefaultHttpClient(String trustStoreType, String trustStorePath, String trustStorePassword) {
        this.trustStoreType = trustStoreType;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;

        this.useTrustManager = StringHelper.isNotEmpty(trustStoreType) && StringHelper.isNotEmpty(trustStorePath)
                && StringHelper.isNotEmpty(trustStorePassword);
    }

    public SslDefaultHttpClient(String trustStoreType, String trustStorePath, String trustStorePassword,
            String keyStoreType, String keyStorePath, String keyStorePassword) {
        this(trustStoreType, trustStorePath, trustStorePassword);

        this.keyStoreType = keyStoreType;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;

        this.useKeyManager = StringHelper.isNotEmpty(keyStoreType) && StringHelper.isNotEmpty(keyStorePath)
                && StringHelper.isNotEmpty(keyStorePassword);
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        // Register for port 443 our SSLSocketFactory with our keystore to the
        // ConnectionManager
        registry.register(new Scheme("https", 443, newSslSocketFactory()));

        return new PoolingClientConnectionManager(registry);
    }

    private SSLSocketFactory newSslSocketFactory() {
        try {
            TrustManager[] trustManagers = this.trustManagers;
            if (useTrustManager) {
                trustManagers = getTrustManagers();
            }

            KeyManager[] keyManagers = null;
            if (useKeyManager) {
                keyManagers = getKeyManagers();
            }

            SSLContext ctx = SSLContext.getInstance("TLS");

            ctx.init(keyManagers, trustManagers, new SecureRandom());

            // Pass the keystore to the SSLSocketFactory
            SSLSocketFactory sf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            return sf;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to load keystore", ex);
        }

    }

    private TrustManager[] getTrustManagers() throws Exception {
        KeyStore keyStore = getKeyStore(this.trustStoreType, this.trustStorePath, this.trustStorePassword);

        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(keyStore);

        return tmFactory.getTrustManagers();
    }

    private KeyManager[] getKeyManagers() throws Exception {
        KeyStore keyStore = getKeyStore(this.keyStoreType, this.keyStorePath, this.keyStorePassword);

        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmFactory.init(keyStore, this.keyStorePassword.toCharArray());

        return kmFactory.getKeyManagers();
    }

    private KeyStore getKeyStore(String storeType, String storePath, String storePassword) throws Exception {
        InputStream keyStoreInput = FileUtils.openInputStream(new File(storePath));

        try {
            KeyStore keyStore = KeyStore.getInstance(storeType);
            keyStore.load(keyStoreInput, storePassword.toCharArray());

            return keyStore;
        } finally {
            IOUtils.closeQuietly(keyStoreInput);
        }
    }

}

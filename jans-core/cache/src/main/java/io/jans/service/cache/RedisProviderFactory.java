/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Important : keep it weld free. It's reused by oxd !
 *
 * @author yuriyz
 */
public final class RedisProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RedisProviderFactory.class);

    private RedisProviderFactory() {
    }

    public static AbstractRedisProvider create(CacheConfiguration configuration) {
        try {
            LOG.debug("Creating RedisProvider ... configuration:" + configuration);

            switch (configuration.getRedisConfiguration().getRedisProviderType()) {
                case STANDALONE:
                    return new RedisStandaloneProvider(configuration);
                case CLUSTER:
                    return new RedisClusterProvider(configuration);
                case SHARDED:
                    return new RedisShardedProvider(configuration);
                case SENTINEL:
                    return new RedisSentinelProvider(configuration);
                default:
                    LOG.error("Failed to create RedisProvider. RedisProviderType is not supported by current version of Janssen Project: "
                            + configuration.getRedisConfiguration().getRedisProviderType() + ", redisConfiguration:" + configuration);
                    throw new RuntimeException(
                            "RedisProviderType is not supported by current version of Janssen Project: " + configuration.getRedisConfiguration().getRedisProviderType());
            }
        } catch (Exception e) {
            LOG.error("Failed to create RedisProvider.");
            throw new RuntimeException("Error creating RedisProvider", e);
        }
    }

    public static void destroy(AbstractRedisProvider provider) {
        if (provider != null) {
            provider.destroy();
        }
    }

    public static void destroySilently(AbstractRedisProvider provider) {
        try {
            destroy(provider);
        } catch (Exception e) {
            LOG.error("Failed to destroy redis provider.", e);
        }
    }

    public static SSLSocketFactory createSslSocketFactory(RedisConfiguration redisConfiguration) throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        if (StringUtils.isNotBlank(redisConfiguration.getSslTrustStoreFilePath())) {
            trustManagerFactory.init(loadKeyStore(redisConfiguration.getSslTrustStoreFilePath(), redisConfiguration.getSslTrustStorePassword()));
        } else {
            trustManagerFactory.init((KeyStore) null);
        }

        KeyManager[] keyManagers = null;
        if (StringUtils.isNotBlank(redisConfiguration.getSslKeyStoreFilePath())) {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(loadKeyStore(redisConfiguration.getSslKeyStoreFilePath(), redisConfiguration.getSslKeyStorePassword()),
                    toCharArray(redisConfiguration.getSslKeyStorePassword()));
            keyManagers = keyManagerFactory.getKeyManagers();
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(), new SecureRandom());

        return sslContext.getSocketFactory();
    }

    public static SSLParameters createSslParameters() {
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        return sslParameters;
    }

    public static HostnameVerifier createHostnameVerifier() {
        return new DefaultHostnameVerifier();
    }

    private static KeyStore loadKeyStore(String path, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream is = new FileInputStream(path)) {
            keyStore.load(is, toCharArray(password));
        }
        return keyStore;
    }

    private static char[] toCharArray(String password) {
        return password == null ? null : password.toCharArray();
    }

    public static void setSSLSystemProperties(RedisConfiguration redisConfiguration) {
        if (StringUtils.isNotBlank(redisConfiguration.getSslKeyStoreFilePath())) {
            System.setProperty("javax.net.ssl.keyStore", redisConfiguration.getSslKeyStoreFilePath());
            System.setProperty("javax.net.ssl.keyStorePassword", redisConfiguration.getSslKeyStorePassword());
        }

        if (StringUtils.isNotBlank(redisConfiguration.getSslTrustStoreFilePath())) {
            System.setProperty("javax.net.ssl.trustStore", redisConfiguration.getSslTrustStoreFilePath());
            System.setProperty("javax.net.ssl.trustStorePassword", redisConfiguration.getSslTrustStorePassword());
        }
    }
}

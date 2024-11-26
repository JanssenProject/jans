/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
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

    public static AbstractRedisProvider create(RedisConfiguration redisConfiguration) {
        try {
            LOG.debug("Creating RedisProvider ... configuration:" + redisConfiguration);

            switch (redisConfiguration.getRedisProviderType()) {
                case STANDALONE:
                    return new RedisStandaloneProvider(redisConfiguration);
                case CLUSTER:
                    return new RedisClusterProvider(redisConfiguration);
                case SHARDED:
                    return new RedisShardedProvider(redisConfiguration);
                case SENTINEL:
                    return new RedisSentinelProvider(redisConfiguration);
                default:
                    LOG.error("Failed to create RedisProvider. RedisProviderType is not supported by current version of Janssen Project: "
                            + redisConfiguration.getRedisProviderType() + ", redisConfiguration:" + redisConfiguration);
                    throw new RuntimeException(
                            "RedisProviderType is not supported by current version of Janssen Project: " + redisConfiguration.getRedisProviderType());
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
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(new FileInputStream(redisConfiguration.getSslTrustStoreFilePath()),
                redisConfiguration.getSslTrustStorePassword().toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(redisConfiguration.getSslKeyStoreFilePath()),
                redisConfiguration.getSslKeyStorePassword().toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, redisConfiguration.getSslKeyStorePassword().toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

        return sslContext.getSocketFactory();
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

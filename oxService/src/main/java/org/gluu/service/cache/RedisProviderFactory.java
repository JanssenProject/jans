package org.gluu.service.cache;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
            default:
                LOG.error("Failed to create RedisProvider. RedisProviderType is not supported by current version of oxcore: "
                        + redisConfiguration.getRedisProviderType() + ", redisConfiguration:" + redisConfiguration);
                throw new RuntimeException(
                        "RedisProviderType is not supported by current version of oxcore: " + redisConfiguration.getRedisProviderType());
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

    public static SSLSocketFactory createTrustStoreSslSocketFactory(File keystoreFile) throws Exception {

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(keystoreFile);
            trustStore.load(inputStream, null);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }
}

package org.xdi.oxd.server.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.JwkClient;
import org.xdi.oxauth.client.JwkResponse;
import org.xdi.oxauth.model.crypto.PublicKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.util.Pair;

import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/04/2017
 */

public class KeyService {

    private static final Logger LOG = LoggerFactory.getLogger(KeyService.class);

    private final Cache<Pair<String, String>, RSAPublicKey> cache;

    @Inject
    public KeyService(ConfigurationService configurationService) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(configurationService.get().getKeyExpirationInMinutes(), TimeUnit.MINUTES)
                .build();
    }


    public RSAPublicKey getRSAPublicKey(String jwkSetUri, String keyId) {
        try {
            final Pair<String, String> mapKey = new Pair<>(jwkSetUri, keyId);

            RSAPublicKey cachedKey = cache.getIfPresent(mapKey);
            if (cachedKey != null) {
                LOG.debug("Taken public key from cache, mapKey: " + mapKey);
                return cachedKey;
            }

            RSAPublicKey publicKey = null;

            JwkClient jwkClient = new JwkClient(jwkSetUri);
            jwkClient.setExecutor(new ApacheHttpClient4Executor(CoreUtils.createHttpClientTrustAll()));
            JwkResponse jwkResponse = jwkClient.exec();
            if (jwkResponse != null && jwkResponse.getStatus() == 200) {
                PublicKey pk = jwkResponse.getPublicKey(keyId);
                if (pk instanceof RSAPublicKey) {
                    publicKey = (RSAPublicKey) pk;
                    cache.put(mapKey, publicKey);
                }
            }

            return publicKey;
        } catch (Exception e) {
            LOG.error("Failed to fetch public key.", e);
            throw new RuntimeException("Failed to fetch public key.", e);
        }
    }

    public RSAPublicKey refetchKey(String jwkUrl, String kid) {
        cache.invalidate(new Pair<>(jwkUrl, kid));
        return getRSAPublicKey(jwkUrl, kid);
    }
}

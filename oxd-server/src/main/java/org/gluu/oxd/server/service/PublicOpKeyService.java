package org.gluu.oxd.server.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.JwkClient;
import org.gluu.oxauth.client.JwkResponse;
import org.gluu.oxauth.model.crypto.PublicKey;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.util.Pair;

import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 */

public class PublicOpKeyService {

    private static final Logger LOG = LoggerFactory.getLogger(PublicOpKeyService.class);

    private final Cache<Pair<String, String>, RSAPublicKey> cache;
    private final HttpService httpService;

    @Inject
    public PublicOpKeyService(ConfigurationService configurationService, HttpService httpService) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(configurationService.get().getPublicOpKeyCacheExpirationInMinutes(), TimeUnit.MINUTES)
                .build();
        this.httpService = httpService;
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
            jwkClient.setExecutor(new ApacheHttpClient4Executor(httpService.getHttpClient()));
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

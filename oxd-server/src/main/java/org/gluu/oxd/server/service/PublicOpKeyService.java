package org.gluu.oxd.server.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.dropwizard.util.Strings;
import org.gluu.oxauth.client.JwkClient;
import org.gluu.oxauth.client.JwkResponse;
import org.gluu.oxauth.model.crypto.PublicKey;
import org.gluu.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.JSONWebKey;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;
import org.gluu.oxauth.model.jwk.Use;
import org.gluu.oxd.server.op.OpClientFactory;
import org.gluu.util.Pair;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 */

public class PublicOpKeyService {

    private static final Logger LOG = LoggerFactory.getLogger(PublicOpKeyService.class);

    private final Cache<Pair<String, String>, PublicKey> cache;
    private final HttpService httpService;
    private OpClientFactory opClientFactory;

    @Inject
    public PublicOpKeyService(ConfigurationService configurationService, HttpService httpService, OpClientFactory opClientFactory) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(configurationService.get().getPublicOpKeyCacheExpirationInMinutes(), TimeUnit.MINUTES)
                .build();
        this.httpService = httpService;
        this.opClientFactory = opClientFactory;
    }

    public PublicKey getPublicKey(String jwkSetUrl, String keyId, SignatureAlgorithm signatureAlgorithm, Use use) {
        //Get keys from cache if present
        Optional<PublicKey> cachedKey = getCachedKey(jwkSetUrl, keyId);

        if (cachedKey.isPresent()) {
            LOG.debug("Taken public key from cache. jwks_url: {}, kid : {} ", jwkSetUrl, keyId);
            return cachedKey.get();
        }
        //Request jwks from OP
        JwkClient jwkClient = opClientFactory.createJwkClient(jwkSetUrl);
        jwkClient.setExecutor(new ApacheHttpClient4Executor(httpService.getHttpClient()));

        JwkResponse jwkResponse = jwkClient.exec();
        if (jwkResponse == null || jwkResponse.getStatus() != 200) {
            LOG.error("Failed to fetch public key from OP. Obtained Response : {}", (jwkResponse == null ? jwkResponse : jwkResponse.getStatus()));
            throw new RuntimeException("Failed to fetch public key from OP. Obtained Response : " + (jwkResponse == null ? jwkResponse : jwkResponse.getStatus()));
        }

        if (!Strings.isNullOrEmpty(keyId)) {
            PublicKey publicKey = jwkResponse.getPublicKey(keyId);
            if (publicKey != null) {
                cache.put((new Pair<>(jwkSetUrl, keyId)), publicKey);
                return publicKey;
            }

        } else {
            JSONWebKeySet jsonWebKeySet = jwkResponse.getJwks();
            List<PublicKey> pks = Lists.newArrayList();
            for (JSONWebKey key : jsonWebKeySet.getKeys()) {

                if (key.getKty() == null)
                    continue;

                if (signatureAlgorithm.getFamily().toString().equals(key.getKty().toString()) && (use == null || use == key.getUse())) {
                    pks.add(getPublicKey(key));
                }
            }

            if (pks.size() > 1) {
                LOG.error("Multiple matching keys found in issuer's jwks_uri for algorithm : {}. `kid` must be provided in this case.", signatureAlgorithm.getName());
                throw new RuntimeException("Multiple matching keys found in issuer's jwks_uri for algorithm : " + signatureAlgorithm.getName() + ". `kid` must be provided in this case.");
            }

            if (pks.size() == 1) {
                if (!Strings.isNullOrEmpty(pks.get(0).getKeyId())) {
                    cache.put((new Pair<>(jwkSetUrl, pks.get(0).getKeyId())), pks.get(0));
                }

                return pks.get(0);
            }
        }
        LOG.error("Failed to fetch public key from OP.");
        throw new RuntimeException("Failed to fetch public key from OP.");
    }

    private Optional<PublicKey> getCachedKey(String jwkSetUrl, String keyId) {
        if (Strings.isNullOrEmpty(keyId)) {
            return Optional.empty();
        }
        Pair<String, String> mapKey = new Pair<>(jwkSetUrl, keyId);
        return Optional.ofNullable(cache.getIfPresent(mapKey));
    }

    public PublicKey getPublicKey(JSONWebKey jsonWebKey) {
        PublicKey publicKey = null;

        if (jsonWebKey != null) {
            switch (jsonWebKey.getKty()) {
                case RSA:
                    publicKey = new RSAPublicKey(
                            jsonWebKey.getN(),
                            jsonWebKey.getE());
                    break;
                case EC:
                    publicKey = new ECDSAPublicKey(
                            SignatureAlgorithm.fromString(jsonWebKey.getAlg().getParamName()),
                            jsonWebKey.getX(),
                            jsonWebKey.getY());
                    break;
                default:
                    break;
            }
        }

        return publicKey;
    }
}

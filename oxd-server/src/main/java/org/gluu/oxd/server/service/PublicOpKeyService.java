package org.gluu.oxd.server.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import org.gluu.oxauth.client.JwkClient;
import org.gluu.oxauth.client.JwkResponse;
import org.gluu.oxauth.model.crypto.PublicKey;
import org.gluu.oxauth.model.crypto.signature.AlgorithmFamily;
import org.gluu.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.JSONWebKey;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;
import org.gluu.oxauth.model.jwk.Use;
import org.gluu.oxauth.model.jws.ECDSASigner;
import org.gluu.oxauth.model.jws.RSASigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.server.op.OpClientFactory;
import org.gluu.util.Pair;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public PublicKey getPublicKey(String jwkSetUrl, String keyId) {
        try {
            PublicKey publicKey = null;

            final Pair<String, String> mapKey = new Pair<>(jwkSetUrl, keyId);

            PublicKey cachedKey = cache.getIfPresent(mapKey);
            if (cachedKey != null) {
                LOG.debug("Taken public key from cache, mapKey: " + mapKey);
                return cachedKey;
            }

            JwkClient jwkClient = opClientFactory.createJwkClient(jwkSetUrl);
            jwkClient.setExecutor(new ApacheHttpClient4Executor(httpService.getHttpClient()));

            JwkResponse jwkResponse = jwkClient.exec();
            if (jwkResponse != null && jwkResponse.getStatus() == 200) {
                publicKey = jwkResponse.getPublicKey(keyId);
                cache.put(mapKey, publicKey);
            }

            return publicKey;
        } catch (Exception e) {
            LOG.error("Failed to fetch public key.", e);
            throw new RuntimeException("Failed to fetch public key.", e);
        }
    }

    public String getKeyId(Jwt idToken, String jwkSetUri, SignatureAlgorithm signatureAlgorithm, Use use) {
        try {
            JwkClient jwkClient = opClientFactory.createJwkClient(jwkSetUri);
            jwkClient.setExecutor(new ApacheHttpClient4Executor(httpService.getHttpClient()));
            JwkResponse jwkResponse = jwkClient.exec();

            if (jwkResponse != null && jwkResponse.getStatus() == 200) {
                JSONWebKeySet jsonWebKeySet = jwkResponse.getJwks();

                for (JSONWebKey key : jsonWebKeySet.getKeys()) {
                    if (signatureAlgorithm.getFamily().toString().equals(key.getKty().toString()) && (use == null || use == key.getUse())) {
                        String kid = key.getKid();
                        PublicKey pk = jwkResponse.getPublicKey(kid);

                        if (signatureAlgorithm.getFamily().toString().equals(AlgorithmFamily.RSA.toString())) {
                            if (pk instanceof RSAPublicKey) {
                                final Pair<String, String> mapKey = new Pair<>(jwkSetUri, kid);
                                RSAPublicKey publicKey = (RSAPublicKey) pk;
                                RSASigner rsaSigner = new RSASigner(signatureAlgorithm, publicKey);
                                boolean isValid = rsaSigner.validate(idToken);
                                if (isValid) {
                                    cache.put(mapKey, publicKey);
                                    return kid;
                                }
                            }
                        } else if (signatureAlgorithm.getFamily().toString().equals(AlgorithmFamily.EC.toString())) {
                            if (pk instanceof ECDSAPublicKey) {
                                final Pair<String, String> mapKey = new Pair<>(jwkSetUri, kid);
                                ECDSAPublicKey publicKey = (ECDSAPublicKey) pk;
                                ECDSASigner ecdsaSigner = new ECDSASigner(signatureAlgorithm, publicKey);
                                boolean isValid = ecdsaSigner.validate(idToken);
                                if (isValid) {
                                    cache.put(mapKey, publicKey);
                                    return kid;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("`kid` is missing in Id_Token. Error in getting `kid` from keystore.", e);
            throw new RuntimeException("`kid` is missing in Id_Token. Error in getting `kid` from keystore.", e);
        }

        LOG.warn("`kid` is missing in `Id_Token`. Unable to find matching key out of the Issuer's published set, algorithm family: " + signatureAlgorithm.getFamily() + ", use: " + use.toString());
        return null;
    }

    public PublicKey refetchKey(String jwkUrl, String kid) {
        cache.invalidate(new Pair<>(jwkUrl, kid));
        return getPublicKey(jwkUrl, kid);
    }
}

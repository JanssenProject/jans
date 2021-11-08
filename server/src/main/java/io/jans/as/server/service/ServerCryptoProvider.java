/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.Use;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.service.cdi.util.CdiUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.msgpack.core.Preconditions;

import java.security.KeyStoreException;
import java.security.PrivateKey;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ServerCryptoProvider extends AbstractCryptoProvider {

    private static final Logger LOG = Logger.getLogger(ServerCryptoProvider.class);

    private final ConfigurationFactory configurationFactory;
    private final AbstractCryptoProvider cryptoProvider;

    public ServerCryptoProvider(AbstractCryptoProvider cryptoProvider) {
        this.configurationFactory = CdiUtil.bean(ConfigurationFactory.class);
        this.cryptoProvider = cryptoProvider;
        Preconditions.checkNotNull(configurationFactory);
        Preconditions.checkNotNull(cryptoProvider);
    }

    @Override
    public String getKeyId(JSONWebKeySet jsonWebKeySet, Algorithm algorithm, Use use) throws Exception {
        try {
            if (algorithm == null || AlgorithmFamily.HMAC.equals(algorithm.getFamily())) {
                return null;
            }

            final AppConfiguration appConfiguration = configurationFactory.getAppConfiguration();
            if (appConfiguration.getKeySignWithSameKeyButDiffAlg()) { // open banking: same key with different algorithms
                LOG.trace("Getting key by use: " + use);
                for (JSONWebKey key : jsonWebKeySet.getKeys()) {
                    if (use != null && use == key.getUse()) {
                        LOG.trace("Found " + key.getKid() + ", use: " + use);
                        return key.getKid();
                    }
                }
            }

            final String staticKid = appConfiguration.getStaticKid();
            if (StringUtils.isNotBlank(staticKid)) {
                LOG.trace("Use staticKid: " + staticKid);
                return staticKid;
            }

            final String kid = cryptoProvider.getKeyId(jsonWebKeySet, algorithm, use);
            if (!cryptoProvider.getKeys().contains(kid) && configurationFactory.reloadConfFromLdap()) {
                return cryptoProvider.getKeyId(jsonWebKeySet, algorithm, use);
            }
            return kid;

        } catch (KeyStoreException e) {
            LOG.trace("Try to re-load configuration due to keystore exception (it can be rotated).");
            if (configurationFactory.reloadConfFromLdap()) {
                return cryptoProvider.getKeyId(jsonWebKeySet, algorithm, use);
            }
        }
        return null;
    }

    @Override
    public JSONObject generateKey(Algorithm algorithm, Long expirationTime, Use use) throws Exception {
        return cryptoProvider.generateKey(algorithm, expirationTime, use);
    }

    @Override
    public String sign(String signingInput, String keyId, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception {
        if (configurationFactory.getAppConfiguration().getRejectJwtWithNoneAlg() && signatureAlgorithm == SignatureAlgorithm.NONE) {
            throw new UnsupportedOperationException("None algorithm is forbidden by `rejectJwtWithNoneAlg` configuration property.");
        }
        return cryptoProvider.sign(signingInput, keyId, sharedSecret, signatureAlgorithm);
    }

    @Override
    public boolean verifySignature(String signingInput, String encodedSignature, String keyId, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws Exception {
        if (configurationFactory.getAppConfiguration().getRejectJwtWithNoneAlg() && signatureAlgorithm == SignatureAlgorithm.NONE) {
            LOG.trace("None algorithm is forbidden by `rejectJwtWithNoneAlg` configuration property.");
            return false;
        }
        return cryptoProvider.verifySignature(signingInput, encodedSignature, keyId, jwks, sharedSecret, signatureAlgorithm);
    }

    @Override
    public boolean deleteKey(String keyId) throws Exception {
        return cryptoProvider.deleteKey(keyId);
    }

    @Override
    public boolean containsKey(String keyId) {
        return cryptoProvider.containsKey(keyId);
    }

    @Override
    public PrivateKey getPrivateKey(String keyId) throws Exception {
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

        if (privateKey == null) {
            final AppConfiguration appConfiguration = configurationFactory.getAppConfiguration();
            if (StringUtils.isNotBlank(appConfiguration.getStaticDecryptionKid())) {
                privateKey = cryptoProvider.getPrivateKey(appConfiguration.getStaticDecryptionKid());
            }
        }

        return privateKey;
    }
}

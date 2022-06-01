package io.jans.ca.server.service;

import com.google.common.collect.Lists;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.Util;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObject;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.ApiAppConfiguration;
import io.jans.ca.server.persistence.service.PersistenceServiceImpl;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.security.KeyStoreException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@ApplicationScoped
public class KeyGeneratorService {

    @Inject
    Logger logger;

    @Inject
    PersistenceServiceImpl persistenceService;

    @Inject
    MainPersistenceService jansConfigurationService;

    private JSONWebKeySet keys;

    public AbstractCryptoProvider getCryptoProvider() throws KeyStoreException {
        ApiAppConfiguration configuration = getConfiguration();
        try {
            return new AuthCryptoProvider(configuration.getCryptProviderKeyStorePath(), configuration.getCryptProviderKeyStorePassword(), configuration.getCryptProviderDnName());
        } catch (Exception e) {
            logger.error("Failed to create CryptoProvider.", e);
            throw new RuntimeException("Failed to create CryptoProvider.", e);
        }
    }

    private ApiAppConfiguration getConfiguration() {
        return jansConfigurationService.find();
    }


    public void generateKeys() {

        List<Algorithm> signatureAlgorithms = Lists.newArrayList(Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256,
                Algorithm.ES384, Algorithm.ES512, Algorithm.PS256, Algorithm.PS384, Algorithm.PS512);

        List<Algorithm> encryptionAlgorithms = Lists.newArrayList(Algorithm.RSA1_5, Algorithm.RSA_OAEP);
        ApiAppConfiguration configuration = getConfiguration();
        try {
            if (configuration.getEnableJwksGeneration()) {
                JSONWebKeySet keySet = generateKeys(signatureAlgorithms, encryptionAlgorithms, configuration.getJwksExpirationInHours());
                saveKeysInStorage(keySet.toString());
                setKeys(keySet);
            }
        } catch (Exception e) {
            logger.error("Failed to generate json web keys.", e);
            throw new RuntimeException("Failed to generate json web keys.", e);
        }
    }

    private JSONWebKeySet generateKeys(List<Algorithm> signatureAlgorithms,
                                       List<Algorithm> encryptionAlgorithms, int expiration_hours) throws KeyStoreException {
        logger.trace("Generating jwks keys...");
        JSONWebKeySet jwks = new JSONWebKeySet();

        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.HOUR, expiration_hours);

        AbstractCryptoProvider cryptoProvider = getCryptoProvider();
        for (Algorithm algorithm : signatureAlgorithms) {
            try {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.name());
                JSONObject result = cryptoProvider.generateKey(algorithm, calendar.getTimeInMillis());

                JSONWebKey key = JSONWebKey.fromJSONObject(result);
                jwks.getKeys().add(key);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        for (Algorithm algorithm : encryptionAlgorithms) {
            try {
                KeyEncryptionAlgorithm encryptionAlgorithm = KeyEncryptionAlgorithm.fromName(algorithm.getParamName());
                JSONObject result = cryptoProvider.generateKey(algorithm,
                        calendar.getTimeInMillis());

                JSONWebKey key = JSONWebKey.fromJSONObject(result);
                jwks.getKeys().add(key);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        logger.trace("jwks generated successfully.");
        return jwks;
    }

    public Jwt sign(Jwt jwt, String sharedSecret, SignatureAlgorithm signatureAlgorithm) {
        try {
            String signature = getCryptoProvider().sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), sharedSecret, signatureAlgorithm);
            jwt.setEncodedSignature(signature);
            //return signed jwt
            return jwt;
        } catch (Exception e) {
            logger.error("Failed to sign signingInput.", e);
            throw new RuntimeException("Failed to signingInput.", e);
        }
    }

    public JSONWebKeySet getKeys() {
        ApiAppConfiguration configuration = getConfiguration();
        if (configuration.getEnableJwksGeneration()) {
            logger.info("Keys found: {}", keys);
            if (keys != null && !keys.getKeys().isEmpty()) {
                return this.keys;
            }
            //if keys not found then search in storage
            JSONWebKeySet keys = getKeysFromStorage();
            if (keys != null && !keys.getKeys().isEmpty()) {
                this.keys = keys;
                return this.keys;
            }
            //generate new keys in case they do not exist
            generateKeys();
            return this.keys;
        }
        logger.info("Relying party JWKS generation is disabled in running jans_client_api instance. To enable it set `enableJwksGeneration` field to true in ApiAppConfiguration.");
        throw new HttpException(ErrorResponseCode.JWKS_GENERATION_DISABLE);
    }

    public void setKeys(JSONWebKeySet keys) {
        this.keys = keys;
    }

    public String getKeyId(Algorithm algorithm, Use use) {
        try {
            AbstractCryptoProvider cryptoProvider = getCryptoProvider();
            final String kid = cryptoProvider.getKeyId(getKeys(), algorithm, use);
            if (!cryptoProvider.getKeys().contains(kid)) {
                return cryptoProvider.getKeyId(getKeys(), algorithm, use);
            }
            return kid;

        } catch (CryptoProviderException e) {
            logger.error("Error in keyId generation");

        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void saveKeysInStorage(String jwks) {
        persistenceService.createExpiredObject(new ExpiredObject(ExpiredObjectType.JWKS.getValue(), jwks, ExpiredObjectType.JWKS, getConfiguration().getJwksExpirationInHours() * 60));
    }

    public JSONWebKeySet getKeysFromStorage() {
        ExpiredObject expiredObject = persistenceService.getExpiredObject(ExpiredObjectType.JWKS.getValue());
        logger.info("Expired Object found from Storage: {}", expiredObject);
        if (expiredObject == null || Util.isNullOrEmpty(expiredObject.getValue())) {
            return null;
        }

        JSONObject keysInJson = new JSONObject(expiredObject.getValue());
        JSONWebKeySet keys = JSONWebKeySet.fromJSONObject(keysInJson);
        try {
            if (hasKeysExpired(expiredObject)) {
                logger.trace("The keys in storage got expired. Deleting the expired keys from storage.");
                deleteKeysFromStorage();
                return null;
            }
        } catch (Exception e) {
            logger.error("Error in reading expiry date or deleting expired keys from storage. Trying to delete the keys from storage.", e);
            deleteKeysFromStorage();
            return null;
        }
        return keys;
    }

    public void deleteKeysFromStorage() {
        persistenceService.deleteExpiredObjectsByKey(ExpiredObjectType.JWKS.getValue());
    }

    public boolean hasKeysExpired(ExpiredObject expiredObject) {

        long expirationDate = expiredObject.getExp().getTime();
        long today = new Date().getTime();
        long expiresInMinutes = (expirationDate - today) / (60 * 1000);

        return (expiresInMinutes <= 0);
    }
}

package org.gluu.oxd.server.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.dropwizard.util.Strings;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.Algorithm;
import org.gluu.oxauth.model.jwk.JSONWebKey;
import org.gluu.oxauth.model.jwk.JSONWebKeySet;
import org.gluu.oxauth.model.jwk.Use;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.ExpiredObject;
import org.gluu.oxd.common.ExpiredObjectType;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.persistence.service.PersistenceService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStoreException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class KeyGeneratorService {

    private static final Logger LOG = LoggerFactory.getLogger(KeyGeneratorService.class);

    private OxdServerConfiguration configuration;
    private PersistenceService persistenceService;
    private AbstractCryptoProvider cryptoProvider;

    private JSONWebKeySet keys;

    @Inject
    public KeyGeneratorService(OxdServerConfiguration configuration, PersistenceService persistenceService) {
        this.configuration = configuration;
        this.keys = new JSONWebKeySet();
        this.persistenceService = persistenceService;
        try {
            this.cryptoProvider = new OxAuthCryptoProvider(configuration.getCryptProviderKeyStorePath(), configuration.getCryptProviderKeyStorePassword(), configuration.getCryptProviderDnName());
        } catch (Exception e) {
            LOG.error("Failed to create CryptoProvider.", e);
            throw new RuntimeException("Failed to create CryptoProvider.", e);
        }
    }

    public void generateKeys() {

        List<Algorithm> signatureAlgorithms = Lists.newArrayList(Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256,
                Algorithm.ES384, Algorithm.ES512, Algorithm.PS256, Algorithm.PS384, Algorithm.PS512);

        List<Algorithm> encryptionAlgorithms = Lists.newArrayList(Algorithm.RSA1_5, Algorithm.RSA_OAEP);

        try {
            if (configuration.getEnableJwksGeneration()) {
                JSONWebKeySet keySet = generateKeys(signatureAlgorithms, encryptionAlgorithms, configuration.getJwksExpirationInHours());
                saveKeysInStorage(keySet.toString());
                setKeys(keySet);
            }
        } catch (Exception e) {
            LOG.error("Failed to generate json web keys.", e);
            throw new RuntimeException("Failed to generate json web keys.", e);
        }
    }

    private JSONWebKeySet generateKeys(List<Algorithm> signatureAlgorithms,
                                       List<Algorithm> encryptionAlgorithms, int expiration_hours) throws Exception, JSONException {
        LOG.trace("Generating jwks keys...");
        JSONWebKeySet jwks = new JSONWebKeySet();

        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.HOUR, expiration_hours);

        for (Algorithm algorithm : signatureAlgorithms) {
            try {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.name());
                JSONObject result = this.cryptoProvider.generateKey(algorithm, calendar.getTimeInMillis(), Use.SIGNATURE);

                JSONWebKey key = JSONWebKey.fromJSONObject(result);
                jwks.getKeys().add(key);
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }

        for (Algorithm algorithm : encryptionAlgorithms) {
            try {
                KeyEncryptionAlgorithm encryptionAlgorithm = KeyEncryptionAlgorithm.fromName(algorithm.getParamName());
                JSONObject result = this.cryptoProvider.generateKey(algorithm,
                        calendar.getTimeInMillis(), Use.ENCRYPTION);

                JSONWebKey key = JSONWebKey.fromJSONObject(result);
                jwks.getKeys().add(key);
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }

        //LOG.trace("jwks: ", jwks);
        LOG.trace("jwks generated successfully.");
        return jwks;
    }

    public Jwt sign(Jwt jwt, String sharedSecret, SignatureAlgorithm signatureAlgorithm) {
        try {
            String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), sharedSecret, signatureAlgorithm);
            jwt.setEncodedSignature(signature);
            //return signed jwt
            return jwt;
        } catch (Exception e) {
            LOG.error("Failed to sign signingInput.", e);
            throw new RuntimeException("Failed to signingInput.", e);
        }
    }

    public JSONWebKeySet getKeys() {
        if (configuration.getEnableJwksGeneration()) {
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
        LOG.info("Relying party JWKS generation is disabled in running oxd instance. To enable it set `enable_jwks_generation` field to true in `oxd-server.yml`.");
        throw new HttpException(ErrorResponseCode.JWKS_GENERATION_DISABLE);
    }

    public void setKeys(JSONWebKeySet keys) {
        this.keys = keys;
    }

    public String getKeyId(Algorithm algorithm, Use use) throws Exception {
        try {
            final String kid = cryptoProvider.getKeyId(getKeys(), algorithm, use);
            if (!cryptoProvider.getKeys().contains(kid)) {
                return cryptoProvider.getKeyId(getKeys(), algorithm, use);
            }
            return kid;

        } catch (KeyStoreException e) {
            LOG.error("Error in keyId generation");

        }
        return null;
    }

    public void saveKeysInStorage(String jwks) {
        persistenceService.createExpiredObject(new ExpiredObject(ExpiredObjectType.JWKS.getValue(), jwks, ExpiredObjectType.JWKS, configuration.getJwksExpirationInHours() * 60));
    }

    public JSONWebKeySet getKeysFromStorage() {
        ExpiredObject expiredObject = persistenceService.getExpiredObject(ExpiredObjectType.JWKS.getValue());

        if (expiredObject == null || Strings.isNullOrEmpty(expiredObject.getValue())) {
            return null;
        }

        JSONObject keysInJson = new JSONObject(expiredObject.getValue());
        JSONWebKeySet keys = JSONWebKeySet.fromJSONObject(keysInJson);
        try {
            if (hasKeysExpired(expiredObject)) {
                LOG.trace("The keys in storage got expired. Deleting the expired keys from storage.");
                deleteKeysFromStorage();
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error in reading expiry date or deleting expired keys from storage. Trying to delete the keys from storage.", e);
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

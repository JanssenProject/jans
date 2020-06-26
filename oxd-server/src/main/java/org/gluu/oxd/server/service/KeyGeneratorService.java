package org.gluu.oxd.server.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwk.*;
import org.gluu.oxauth.model.util.StringUtils;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStoreException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.gluu.oxauth.model.jwk.JWKParameter.*;

public class KeyGeneratorService {

    private static final Logger LOG = LoggerFactory.getLogger(KeyGeneratorService.class);

    private OxdServerConfiguration configuration;

    private AbstractCryptoProvider cryptoProvider;

    private JSONWebKeySet keys;

    @Inject
    public KeyGeneratorService(OxdServerConfiguration configuration) {
        this.configuration = configuration;
        this.keys = new JSONWebKeySet();
        try {
            this.cryptoProvider = new OxAuthCryptoProvider(configuration.getCryptProviderKeyStorePath(), configuration.getCryptProviderKeyStorePassword(), configuration.getCryptProviderDnName());
        } catch (Exception e) {
            LOG.error("Failed to create CryptoProvider.", e);
            throw new RuntimeException("Failed to create CryptoProvider.", e);
        }
    }

    public void load() {
        List<Algorithm> signatureAlgorithms = Lists.newArrayList(Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256,
                Algorithm.ES384, Algorithm.ES512, Algorithm.PS256, Algorithm.PS384, Algorithm.PS512);

        List<Algorithm> encryptionAlgorithms = Lists.newArrayList(Algorithm.RSA1_5, Algorithm.RSA_OAEP);

        try {
            if (configuration.getEnableJwksGeneration()) {
                this.keys = generateKeys(signatureAlgorithms, encryptionAlgorithms, configuration.getJwksExpirationInDays(), configuration.getJwksExpirationInHours());
            }
        } catch (Exception e) {
            LOG.error("Failed to generate json web keys.", e);
            throw new RuntimeException("Failed to generate json web keys.", e);
        }
    }

    private JSONWebKeySet generateKeys(List<Algorithm> signatureAlgorithms,
                                       List<Algorithm> encryptionAlgorithms, int expiration, int expiration_hours) throws Exception, JSONException {
        JSONWebKeySet jwks = new JSONWebKeySet();

        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.DATE, expiration);
        calendar.add(Calendar.HOUR, expiration_hours);

        for (Algorithm algorithm : signatureAlgorithms) {
            try {
                SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm.name());
                JSONObject result = this.cryptoProvider.generateKey(algorithm, calendar.getTimeInMillis(), Use.SIGNATURE);

                JSONWebKey key = new JSONWebKey();
                key.setKid(result.getString(KEY_ID));
                key.setUse(Use.SIGNATURE);
                key.setAlg(algorithm);
                key.setKty(KeyType.fromString(signatureAlgorithm.getFamily().toString()));
                key.setExp(result.optLong(EXPIRATION_TIME));
                key.setCrv(signatureAlgorithm.getCurve());
                key.setN(result.optString(MODULUS));
                key.setE(result.optString(EXPONENT));
                key.setX(result.optString(X));
                key.setY(result.optString(Y));

                JSONArray x5c = result.optJSONArray(CERTIFICATE_CHAIN);
                key.setX5c(StringUtils.toList(x5c));

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

                JSONWebKey key = new JSONWebKey();
                key.setKid(result.getString(KEY_ID));
                key.setUse(Use.ENCRYPTION);
                key.setAlg(algorithm);
                key.setKty(KeyType.fromString(encryptionAlgorithm.getFamily()));
                key.setExp(result.optLong(EXPIRATION_TIME));
                key.setN(result.optString(MODULUS));
                key.setE(result.optString(EXPONENT));
                key.setX(result.optString(X));
                key.setY(result.optString(Y));

                JSONArray x5c = result.optJSONArray(CERTIFICATE_CHAIN);
                key.setX5c(StringUtils.toList(x5c));

                jwks.getKeys().add(key);
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }

        //LOG.trace("jwks: ", jwks);
        LOG.trace("jwks generated successfully.");
        return jwks;
    }

    public String sign(String signingInput, String alias, String sharedSecret, SignatureAlgorithm signatureAlgorithm) {
        try {
            return cryptoProvider.sign(signingInput, alias, sharedSecret, signatureAlgorithm);
        } catch (Exception e) {
            LOG.error("Failed to sign signingInput.", e);
            throw new RuntimeException("Failed to signingInput.", e);
        }
    }

    public JSONWebKeySet getKeys() {
        if (configuration.getEnableJwksGeneration()) {
            return keys;
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
}

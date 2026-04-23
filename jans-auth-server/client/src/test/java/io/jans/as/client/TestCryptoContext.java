package io.jans.as.client;

import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.EllipticEdvardsCurve;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jwk.KeyOpsType;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static io.jans.as.model.jwk.JWKParameter.*;

public class TestCryptoContext {

    private static final int DEFAULT_KEY_LENGTH = 2048;
    private static final int DEFAULT_EXPIRATION_HOURS = 24;

    private final AuthCryptoProvider cryptoProvider;
    private final String keyStoreFile;
    private final String keyStoreSecret;
    private final String dnName;
    private final JSONWebKeySet jwks;

    // Key IDs for different algorithms
    private String rs256KeyId;
    private String rs384KeyId;
    private String rs512KeyId;
    private String es256KeyId;
    private String ps256KeyId;

    private TestCryptoContext(AuthCryptoProvider cryptoProvider, String keyStoreFile,
                              String keyStoreSecret, String dnName, JSONWebKeySet jwks) {
        this.cryptoProvider = cryptoProvider;
        this.keyStoreFile = keyStoreFile;
        this.keyStoreSecret = keyStoreSecret;
        this.dnName = dnName;
        this.jwks = jwks;
    }

    private static class Holder {
        private static final TestCryptoContext INSTANCE;

        static {
            try {
                INSTANCE = create();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize TestCryptoContext", e);
            }
        }
    }

    public static TestCryptoContext getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Creates a new TestCryptoContext with generated keys.
     * P12 file is created in temp directory.
     */
    public static TestCryptoContext create() throws Exception {
        return create(Collections.singletonList(Algorithm.RS256));
    }

    /**
     * Creates a new TestCryptoContext with specified algorithms.
     */
    public static TestCryptoContext create(List<Algorithm> algorithms) throws Exception {
        // Create temp p12 file
        File tempFile = Files.createTempFile("test-keystore-", ".p12").toFile();
        tempFile.deleteOnExit();
        String keyStoreFile = tempFile.getAbsolutePath();

        // Create crypto provider (creates p12 if not exists)
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(
                keyStoreFile, TestConstants.DEFAULT_SECRET, TestConstants.DEFAULT_DN_NAME);

        // required to force key store initialization (it's new key store)
        // without it -> java.security.KeyStoreException: Uninitialized keystore
        cryptoProvider.getKeyStore().load(null, null);

        // Generate keys and JWKS
        JSONWebKeySet jwks = new JSONWebKeySet();
        long expirationTime = System.currentTimeMillis() + (DEFAULT_EXPIRATION_HOURS * 60 * 60 * 1000);

        TestCryptoContext context = new TestCryptoContext(
                cryptoProvider, keyStoreFile, TestConstants.DEFAULT_SECRET, TestConstants.DEFAULT_DN_NAME, jwks);

        for (Algorithm algorithm : algorithms) {
            JSONObject keyJson = cryptoProvider.generateKey(
                    algorithm, expirationTime, DEFAULT_KEY_LENGTH, KeyOpsType.CONNECT);

            String keyId = keyJson.getString(KEY_ID);
            context.setKeyIdForAlgorithm(algorithm, keyId);

            // Build JSONWebKey for JWKS
            JSONWebKey jwk = buildJwk(algorithm, keyJson);
            jwks.getKeys().add(jwk);
        }

        return context;
    }

    private void setKeyIdForAlgorithm(Algorithm algorithm, String keyId) {
        switch (algorithm) {
            case RS256:
                this.rs256KeyId = keyId;
                break;
            case RS384:
                this.rs384KeyId = keyId;
                break;
            case RS512:
                this.rs512KeyId = keyId;
                break;
            case ES256:
                this.es256KeyId = keyId;
                break;
            case PS256:
                this.ps256KeyId = keyId;
                break;
            default:
                throw new IllegalStateException("Unsupported algorithm: " + algorithm);
        }
    }

    private static JSONWebKey buildJwk(Algorithm algorithm, JSONObject keyJson) {
        JSONWebKey key = new JSONWebKey();
        key.setKid(keyJson.getString(KEY_ID));
        key.setUse(algorithm.getUse());
        key.setAlg(algorithm);
        key.setKty(algorithm.getFamily().getKeyType());
        key.setExp(keyJson.optLong(EXPIRATION_TIME));
        key.setN(keyJson.optString(MODULUS));
        key.setE(keyJson.optString(EXPONENT));
        key.setX(keyJson.optString(X));
        key.setY(keyJson.optString(Y));
        if (keyJson.has(CURVE)) {
            key.setCrv(EllipticEdvardsCurve.fromString(keyJson.getString(CURVE)));
        }
        JSONArray x5c = keyJson.optJSONArray(CERTIFICATE_CHAIN);
        if (x5c != null) {
            key.setX5c(StringUtils.toList(x5c));
        }
        return key;
    }

    public AuthCryptoProvider getCryptoProvider() {
        return cryptoProvider;
    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    public String getKeyStoreSecret() {
        return keyStoreSecret;
    }

    public String getDnName() {
        return dnName;
    }

    /**
     * Returns a defensive copy of the JWKS to prevent mutation of singleton state.
     */
    public JSONWebKeySet getJwks() {
        return JSONWebKeySet.fromJSONObject(jwks.toJSONObject());
    }

    public String getJwksAsString() {
        return jwks.toString();
    }

    public String getRs256KeyId() {
        return rs256KeyId;
    }

    public String getRs384KeyId() {
        return rs384KeyId;
    }

    public String getRs512KeyId() {
        return rs512KeyId;
    }

    public String getEs256KeyId() {
        return es256KeyId;
    }

    public String getPs256KeyId() {
        return ps256KeyId;
    }

    /**
     * Creates a JWE containing a nested JWS (signed then encrypted).
     *
     * @param authRequest    The authorization request
     * @param sigAlg         Signature algorithm for JWS
     * @param signingKeyId   Key ID for signing
     * @param keyEncAlg      Key encryption algorithm for JWE
     * @param blockEncAlg    Block encryption algorithm for JWE
     * @param encryptionKey  Shared key for symmetric encryption (clientSecret)
     * @param cryptoProvider Crypto provider for signing
     * @param idTokenClaims  Claims to add to id_token
     * @param userInfoClaims Claims to add to userinfo
     * @return Encoded JWE containing nested JWS
     */
    public static String createNestedJwe(
            AuthorizationRequest authRequest,
            SignatureAlgorithm sigAlg,
            String signingKeyId,
            KeyEncryptionAlgorithm keyEncAlg,
            BlockEncryptionAlgorithm blockEncAlg,
            String encryptionKey,
            AuthCryptoProvider cryptoProvider,
            List<Claim> idTokenClaims,
            List<Claim> userInfoClaims) throws Exception {

        if (signingKeyId == null || signingKeyId.isEmpty()) {
            throw new IllegalArgumentException("signingKeyId is required for nested JWE");
        }

        // Step 1: Create and sign JWS
        JwtAuthorizationRequest jwsRequest = new JwtAuthorizationRequest(
                authRequest, sigAlg, cryptoProvider);
        jwsRequest.setKeyId(signingKeyId);

        if (idTokenClaims != null) {
            for (Claim claim : idTokenClaims) {
                jwsRequest.addIdTokenClaim(claim);
            }
        }
        if (userInfoClaims != null) {
            for (Claim claim : userInfoClaims) {
                jwsRequest.addUserInfoClaim(claim);
            }
        }

        Jwt jws = Jwt.parse(jwsRequest.getEncodedJwt());

        // Step 2: Create JWE with nested JWS
        JwtAuthorizationRequest jweRequest = new JwtAuthorizationRequest(
                authRequest, keyEncAlg, blockEncAlg, encryptionKey);
        jweRequest.setNestedPayload(jws);

        return jweRequest.getEncodedJwt();
    }

    /**
     * Simplified version for tests that don't need custom claims.
     */
    public static String createNestedJwe(
            AuthorizationRequest authRequest,
            SignatureAlgorithm sigAlg,
            String signingKeyId,
            KeyEncryptionAlgorithm keyEncAlg,
            BlockEncryptionAlgorithm blockEncAlg,
            String encryptionKey,
            AuthCryptoProvider cryptoProvider) throws Exception {

        return createNestedJwe(authRequest, sigAlg, signingKeyId,
                keyEncAlg, blockEncAlg, encryptionKey, cryptoProvider, null, null);
    }

}

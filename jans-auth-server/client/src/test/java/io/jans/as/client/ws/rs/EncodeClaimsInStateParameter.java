/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.JwtState;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.StringUtils;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;



import static io.jans.as.model.jwt.JwtStateClaimName.ADDITIONAL_CLAIMS;
import static io.jans.as.model.jwt.JwtStateClaimName.JTI;
import static io.jans.as.model.jwt.JwtStateClaimName.KID;
import static io.jans.as.model.jwt.JwtStateClaimName.RFP;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version February 8, 2019
 */
public class EncodeClaimsInStateParameter extends BaseTest {

    private final String additionalClaims = "{first_name: 'Javier', last_name: 'Rojas', age: 34, more: ['foo', 'bar']}";

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void encodeClaimsInStateParameterHS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("encodeClaimsInStateParameterHS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, clientSecret, SignatureAlgorithm.HS256);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void encodeClaimsInStateParameterHS384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("encodeClaimsInStateParameterHS384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.HS384, clientSecret, cryptoProvider);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, clientSecret, SignatureAlgorithm.HS384);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void encodeClaimsInStateParameterHS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("encodeClaimsInStateParameterHS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.HS512, clientSecret, cryptoProvider);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, clientSecret, SignatureAlgorithm.HS512);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "RS256_keyId"})
    @Test
    public void encodeClaimsInStateParameterRS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterRS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.RS256, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.RS256);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "RS384_keyId"})
    @Test
    public void encodeClaimsInStateParameterRS384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterRS384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.RS384, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.RS384);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "RS512_keyId"})
    @Test
    public void encodeClaimsInStateParameterRS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterRS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.RS512, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.RS512);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "ES256_keyId"})
    @Test
    public void encodeClaimsInStateParameterES256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterES256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.ES256, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.ES256);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "ES384_keyId"})
    @Test
    public void encodeClaimsInStateParameterES384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterES384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.ES384, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.ES384);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "ES512_keyId"})
    @Test
    public void encodeClaimsInStateParameterES512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterES512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.ES512, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.ES512);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "PS256_keyId"})
    @Test
    public void encodeClaimsInStateParameterPS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterPS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.PS256, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.PS256);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "PS384_keyId"})
    @Test
    public void encodeClaimsInStateParameterPS384(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterPS384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.PS384, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.PS384);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "PS512_keyId"})
    @Test
    public void encodeClaimsInStateParameterPS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId) throws Exception {
        showTitle("encodeClaimsInStateParameterPS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.PS512, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Validate state
        Jwt jwt = Jwt.parse(state);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.PS512);
        assertTrue(validJwt);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "RS256_keyId", "clientJwksUri"})
    @Test
    public void encodeClaimsInStateParameterAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("encodeClaimsInStateParameterAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        JSONObject jwks = JwtUtil.getJSONWebKeys(clientJwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt(jwks);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Decrypt state
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);
        Jwe jwe = Jwe.parse(state, privateKey, null);
        assertNotNull(jwe.getClaims().getClaimAsString(KID));
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "RS256_keyId", "clientJwksUri"})
    @Test
    public void encodeClaimsInStateParameterAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("encodeClaimsInStateParameterAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        JSONObject jwks = JwtUtil.getJSONWebKeys(clientJwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt(jwks);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Decrypt state
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);
        Jwe jwe = Jwe.parse(state, privateKey, null);
        assertNotNull(jwe.getClaims().getClaimAsString(KID));
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri",
            "keyStoreFile", "keyStoreSecret", "dnName", "RS256_keyId", "clientJwksUri"})
    @Test
    public void encodeClaimsInStateParameterAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String keyStoreFile, final String keyStoreSecret,
            final String dnName, final String keyId, final String clientJwksUri) throws Exception {
        showTitle("encodeClaimsInStateParameterAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        JSONObject jwks = JwtUtil.getJSONWebKeys(clientJwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt(jwks);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Decrypt state
        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);
        Jwe jwe = Jwe.parse(state, privateKey, null);
        assertNotNull(jwe.getClaims().getClaimAsString(KID));
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void encodeClaimsInStateParameterAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("encodeClaimsInStateParameterAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM, clientSecret);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Decrypt state
        Jwe jwe = Jwe.parse(state, null, clientSecret.getBytes());
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void encodeClaimsInStateParameterAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("encodeClaimsInStateParameterAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.A256KW, BlockEncryptionAlgorithm.A256GCM, clientSecret);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));
        String encodedState = jwtState.getEncodedJwt();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(encodedState);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String state = authorizationResponse.getState();

        // 3. Decrypt state
        Jwe jwe = Jwe.parse(state, null, clientSecret.getBytes());
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Test
    public void jwtStateNONETest() throws Exception {
        showTitle("jwtStateNONETest");

        AbstractCryptoProvider cryptoProvider = createCryptoProviderWithAllowedNone();

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.NONE, cryptoProvider);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Encoded State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, null, SignatureAlgorithm.NONE);
        assertTrue(validJwt);
    }

    @Test
    public void jwtStateHS256Test() throws Exception {
        showTitle("jwtStateHS256Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        String sharedKey = "shared_key";

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.HS256, sharedKey, cryptoProvider);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, sharedKey, SignatureAlgorithm.HS256);
        assertTrue(validJwt);
    }

    @Test
    public void jwtStateHS384Test() throws Exception {
        showTitle("jwtStateHS384Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        String sharedKey = "shared_key";

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.HS384, sharedKey, cryptoProvider);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, sharedKey, SignatureAlgorithm.HS384);
        assertTrue(validJwt);
    }

    @Test
    public void jwtStateHS512Test() throws Exception {
        showTitle("jwtStateHS512Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        String sharedKey = "shared_key";

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.HS512, sharedKey, cryptoProvider);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, sharedKey, SignatureAlgorithm.HS512);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "RS256_keyId"})
    @Test
    public void jwtStateRS256Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStateRS256Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.RS256, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.RS256);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "RS384_keyId"})
    @Test
    public void jwtStateRS384Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStateRS384Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.RS384, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.RS384);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "RS512_keyId"})
    @Test
    public void jwtStateRS512Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStateRS512Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.RS512, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.RS512);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "ES256_keyId"})
    @Test
    public void jwtStateES256Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStateES256Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.ES256, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.ES256);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "ES384_keyId"})
    @Test
    public void jwtStateES384Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStateES384Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.ES384, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.ES384);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "ES512_keyId"})
    @Test
    public void jwtStateES512Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStateES512Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.ES512, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.ES512);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "PS256_keyId"})
    @Test
    public void jwtStatePS256Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStatePS256Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.PS256, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.PS256);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "PS384_keyId"})
    @Test
    public void jwtStatePS384Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStatePS384Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.PS384, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.PS384);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "PS512_keyId"})
    @Test
    public void jwtStatePS512Test(final String keyStoreFile, final String keyStoreSecret,
                                  final String dnName, final String keyId) throws Exception {
        showTitle("jwtStatePS512Test");

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(SignatureAlgorithm.PS512, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Signed JWS State: " + encodedState);

        Jwt jwt = Jwt.parse(encodedState);
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                null, null, SignatureAlgorithm.PS512);
        assertTrue(validJwt);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "RS256_keyId", "clientJwksUri"})
    @Test
    public void jwtStateAlgRSAOAEPEncA256GCMTest(
            final String keyStoreFile, final String keyStoreSecret, final String dnName, final String keyId,
            final String clientJwksUri) throws Exception {
        showTitle("jwtStateAlgRSAOAEPEncA256GCMTest");

        JSONObject jwks = JwtUtil.getJSONWebKeys(clientJwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt(jwks);
        assertNotNull(encodedState);
        System.out.println("Encrypted JWE State: " + encodedState);

        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);
        Jwe jwe = Jwe.parse(encodedState, privateKey, null);
        assertNotNull(jwe.getClaims().getClaimAsString(KID));
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "RS256_keyId", "clientJwksUri"})
    @Test
    public void jwtStateAlgRSA15EncA128CBCPLUSHS256Test(
            final String keyStoreFile, final String keyStoreSecret, final String dnName, final String keyId,
            final String clientJwksUri) throws Exception {
        showTitle("jwtStateAlgRSA15EncA128CBCPLUSHS256Test");

        JSONObject jwks = JwtUtil.getJSONWebKeys(clientJwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt(jwks);
        assertNotNull(encodedState);
        System.out.println("Encrypted JWE State: " + encodedState);

        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);
        Jwe jwe = Jwe.parse(encodedState, privateKey, null);
        assertNotNull(jwe.getClaims().getClaimAsString(KID));
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Parameters({"keyStoreFile", "keyStoreSecret", "dnName", "RS256_keyId", "clientJwksUri"})
    @Test
    public void jwtStateAlgRSA15EncA256CBCPLUSHS512Test(
            final String keyStoreFile, final String keyStoreSecret, final String dnName, final String keyId,
            final String clientJwksUri) throws Exception {
        showTitle("jwtStateAlgRSA15EncA256CBCPLUSHS512Test");

        JSONObject jwks = JwtUtil.getJSONWebKeys(clientJwksUri);
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512, cryptoProvider);
        jwtState.setKeyId(keyId);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt(jwks);
        assertNotNull(encodedState);
        System.out.println("Encrypted JWE State: " + encodedState);

        PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);
        Jwe jwe = Jwe.parse(encodedState, privateKey, null);
        assertNotNull(jwe.getClaims().getClaimAsString(KID));
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Test
    public void jwtStateAlgA128KWEncA128GCMTest() throws Exception {
        showTitle("jwtStateAlgA128KWEncA128GCMTest");

        String sharedKey = "shared_key";

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM, sharedKey);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Encrypted JWE State: " + encodedState);

        Jwe jwe = Jwe.parse(encodedState, null, sharedKey.getBytes());
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }

    @Test
    public void jwtStateAlgA256KWEncA256GCMTest() throws Exception {
        showTitle("jwtStateAlgA256KWEncA256GCMTest");

        String sharedKey = "shared_key";

        String rfp = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        JwtState jwtState = new JwtState(KeyEncryptionAlgorithm.A256KW, BlockEncryptionAlgorithm.A256GCM, sharedKey);
        jwtState.setRfp(rfp);
        jwtState.setJti(jti);
        jwtState.setAdditionalClaims(new JSONObject(additionalClaims));

        String encodedState = jwtState.getEncodedJwt();
        assertNotNull(encodedState);
        System.out.println("Encrypted JWE State: " + encodedState);

        Jwe jwe = Jwe.parse(encodedState, null, sharedKey.getBytes());
        assertNotNull(jwe.getClaims().getClaimAsString(RFP));
        assertNotNull(jwe.getClaims().getClaimAsString(JTI));
        assertNotNull(jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS));

        JSONObject addClaims = jwe.getClaims().getClaimAsJSON(ADDITIONAL_CLAIMS);
        assertEquals(addClaims.getString("first_name"), "Javier");
        assertEquals(addClaims.getString("last_name"), "Rojas");
        assertEquals(addClaims.getInt("age"), 34);
        assertNotNull(addClaims.getJSONArray("more"));
        assertEquals(addClaims.getJSONArray("more").length(), 2);
    }
}
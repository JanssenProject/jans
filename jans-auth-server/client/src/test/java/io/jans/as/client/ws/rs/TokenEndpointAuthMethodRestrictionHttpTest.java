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
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static io.jans.as.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static io.jans.as.model.register.RegisterRequestParam.CLIENT_NAME;
import static io.jans.as.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static io.jans.as.model.register.RegisterRequestParam.REDIRECT_URIS;
import static io.jans.as.model.register.RegisterRequestParam.RESPONSE_TYPES;
import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static io.jans.as.model.register.RegisterRequestParam.TOKEN_ENDPOINT_AUTH_METHOD;
import static io.jans.as.model.register.RegisterRequestParam.TOKEN_ENDPOINT_AUTH_SIGNING_ALG;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version February 8, 2019
 */
public class TokenEndpointAuthMethodRestrictionHttpTest extends BaseTest {

    /**
     * Register a client without specify a Token Endpoint Auth Method.
     * Read client to check whether it is using the default Token Endpoint Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void omittedTokenEndpointAuthMethod(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("omittedTokenEndpointAuthMethod");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>client_secret_basic</code>.
     * Read client to check whether it is using the Token Endpoint Auth Method <code>client_secret_basic</code>.
     * Request authorization code.
     * Call to Token Endpoint with Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasic(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretBasic");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_post</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasicFail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretBasicFail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 2: Call to Token Endpoint with Auth Method <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasicFail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretBasicFail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 3: Call to Token Endpoint with Auth Method <code>private_key_jwt</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "RS256_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasicFail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String keyId, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretBasicFail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);


        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>client_secret_post</code>.
     * Read client to check whether it is using the Token Endpoint Auth Method <code>client_secret_post</code>.
     * Request authorization code.
     * Call to Token Endpoint with Auth Method <code>client_secret_post</code>.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPost(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretPost");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_POST.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPostFail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretPostFail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_POST.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 2: Call to Token Endpoint with Auth Method <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPostFail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretPostFail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_POST.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 3: Call to Token Endpoint with Auth Method <code>private_key_jwt</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "RS256_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPostFail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String keyId, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretPostFail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_POST.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);


        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>client_secret_jwt</code>.
     * Read client to check whether it is using the Token Endpoint Auth Method <code>client_secret_jwt</code>.
     * Request authorization code.
     * Call to Token Endpoint with Auth Method <code>client_secret_Jwt</code>.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwt(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwt");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtHS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtHS256");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS256);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtHS384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtHS384");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS384);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * any algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtHS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtHS512");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "RS256_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtRS256Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtRS256Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "RS384_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtRS384Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtRS384Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "RS512_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtRS512Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtRS512Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "ES256_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtES256Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtES256Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "ES384_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtES384Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtES384Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "ES512_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtES512Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtES512Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "PS256_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtPS256Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtPS256Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "PS384_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtPS384Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtPS384Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS384);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * If token_endpoint_auth_signing_alg is omitted in client registration,
     * only symmetric algorithm supported by the OP and the RP can be used.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "PS512_keyId", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtPS512Fail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String keyId,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtPS512Fail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS512);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS256");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS256);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS256);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS256Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS256Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS256);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS384);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS256Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS256Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS256);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS384");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS384);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS384);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS384Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS384Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS384);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS256);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS384Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS384Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS384);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS512");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS512);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS, APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS512Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS512Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS512);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS256);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtSigningAlgHS512Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtSigningAlgHS512Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.HS512);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.HS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS384);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtFail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtFail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 2: Call to Token Endpoint with Auth Method <code>client_secret_post</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtFail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtFail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 3: Call to Token Endpoint with Auth Method <code>private_key_jwt</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "RS256_keyId", "keyStoreFile", "keyStoreSecret",
            "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtFail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String keyId, final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtFail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);


        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>private_key_jwt</code>.
     * Read client to check whether it is using the Token Endpoint Auth Method <code>private_key_jwt</code>.
     * Request authorization code.
     * Call to Token Endpoint with Auth Method <code>private_key_jwt</code>.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwt(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwt");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtFail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtFail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 2: Call to Token Endpoint with Auth Method <code>client_secret_post</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtFail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtFail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail 3: Call to Token Endpoint with Auth Method <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtFail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtFail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtRS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtRS256");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtRS384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtRS384");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtRS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtRS512");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtES256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtES256");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtES384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtES384");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtES512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtES512");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtPS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtPS256");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtPS384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtPS384");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtPS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtPS512");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS256Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS384Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgRS512Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.RS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES256Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES384Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgES512Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.ES512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS256Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS256);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS256.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        readClient.setExecutor(clientEngine(true));
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS384Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS384);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS384.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "PS256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "RS384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail2(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES256_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES384_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail4(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail4");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "ES512_keyId", "dnName",
            "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail5(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtSigningAlgPS512Fail5");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setTokenEndpointAuthSigningAlg(SignatureAlgorithm.PS512);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_SIGNING_ALG.toString()),
                SignatureAlgorithm.PS512.toString());
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
    }
}
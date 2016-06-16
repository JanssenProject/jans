/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public class TokenEndpointAuthMethodRestrictionHttpTest extends BaseTest {

    /**
     * Register a client without specify a Token Endpoint Auth Method.
     * Read client to check whether it is using the default Token Endpoint Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"redirectUris"})
    @Test
    public void omittedTokenEndpointAuthMethod(final String redirectUris) throws Exception {
        showTitle("omittedTokenEndpointAuthMethod");

        // 1. Register client
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse registerResponse = registerClient.execRegister(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>client_secret_basic</code>.
     * Read client to check whether it is using the Token Endpoint Auth Method <code>client_secret_basic</code>.
     * Request authorization code.
     * Call to Token Endpoint with Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasic(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret)
            throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretBasic");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_post</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasicFail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret)
            throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretBasicFail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasicFail2(final String redirectUris, final String redirectUri,
                                                              final String userId, final String userSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretBasicFail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasicFail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String modulus, final String privateExponent) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretBasicFail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS256SIG");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPost(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret)
            throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretPost");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_POST.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPostFail1(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret)
            throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretPostFail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_POST.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPostFail2(final String redirectUris, final String redirectUri,
                                                             final String userId, final String userSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretPostFail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_POST.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPostFail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String modulus, final String privateExponent) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretPostFail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_POST.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS256SIG");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwt(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwt");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtFail1(
            final String redirectUris, final String redirectUri, final String userId,
            final String userSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtFail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtFail2(
            final String redirectUris, final String redirectUri, final String userId,
            final String userSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtFail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtFail3(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String modulus, final String privateExponent) throws Exception {
        showTitle("tokenEndpointAuthMethodClientSecretJwtFail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.CLIENT_SECRET_JWT.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS256SIG");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwt(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret, final String clientJwksUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwt");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        assertNull(authorizationResponse.getIdToken(), "The id token is not null");

        String authorizationCode = authorizationResponse.getCode();

        // 4. Get Access Token
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtFail1(final String redirectUris, final String redirectUri,
                                                          final String userId, final String userSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtFail1");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtFail2(final String redirectUris, final String redirectUri,
                                                          final String userId, final String userSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtFail2");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtFail3(final String redirectUris, final String redirectUri,
                                                          final String userId, final String userSecret) throws Exception {
        showTitle("tokenEndpointAuthMethodPrivateKeyJwtFail3");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertTrue(readClientResponse.getClaims().containsKey(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
        assertEquals(readClientResponse.getClaims().get(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get("scopes"));

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
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
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
}
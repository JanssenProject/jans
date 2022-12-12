/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static io.jans.as.model.register.RegisterRequestParam.CLIENT_NAME;
import static io.jans.as.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static io.jans.as.model.register.RegisterRequestParam.REDIRECT_URIS;
import static io.jans.as.model.register.RegisterRequestParam.RESPONSE_TYPES;
import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Functional tests for Authorize Web Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @version August 29, 2018
 */
public class AuthorizeRestWebServiceHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationCode(final String userId, final String userSecret, final String redirectUris, final String redirectUri, final String sectorIdentifierUri) {
        showTitle("requestAuthorizationCode");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationCodeUserBasicAuth(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationCodeUserBasicAuth");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationCodeNoRedirection(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationCodeNoRedirection");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setUseNoRedirectHeader(true); // Use Alternate Method for redirect
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        showClient(authorizeClient);
        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret"})
    @Test
    public void requestAuthorizationCodeFail1(final String userId, final String userSecret) throws Exception {
        showTitle("requestAuthorizationCodeFail1");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, null, null, null, null);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationCodeFail2(
            final String userId, final String userSecret, final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationCodeFail2");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String redirectUri = "https://INVALID_REDIRECT_URI";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUri"})
    @Test
    public void requestAuthorizationCodeFail3(final String redirectUri) throws Exception {
        showTitle("requestAuthorizationCodeFail3");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Request authorization with an invalid Client ID.
        String clientId = "@!1111!0008!INVALID_VALUE";
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 401, "Unexpected response code: " + authorizationResponse.getStatus());
        assertEquals(authorizationResponse.getErrorType(), AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT);
        assertNotNull(authorizationResponse.getErrorType(), "The error type is null");
        assertNotNull(authorizationResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationCodeFail4(
            final String userId, final String userSecret, final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationCodeFail4");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "email");
        String nonce = UUID.randomUUID().toString();
        String redirectUri = "https://evil.com/oxLicenceAdmin";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);

        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 400, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getErrorType(), "The error type is null");
        assertNotNull(authorizationResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationToken(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationToken");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationTokenUserBasicAuth(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationTokenUserBasicAuth");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
    }

    @Parameters({"userId", "userSecret", "redirectUri"})
    @Test
    public void requestAuthorizationTokenFail1(
            final String userId, final String userSecret, final String redirectUri) throws Exception {
        showTitle("requestAuthorizationTokenFail1");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, null, null, redirectUri, null);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationTokenFail2(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationTokenFail2");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = null;
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationTokenIdToken(
            final String userId, final String userSecret,
            final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationTokenIdToken");

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String accessToken = authorizationResponse.getAccessToken();
        String idToken = authorizationResponse.getIdToken();

        // 2. Validate access_token and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .accessToken(accessToken)
                .claimsPresence(JwtClaimName.ACCESS_TOKEN_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();

    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationTokenIdTokenUserBasicAuth(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationTokenIdTokenUserBasicAuth");

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String accessToken = authorizationResponse.getAccessToken();
        String idToken = authorizationResponse.getIdToken();

        // 2. Validate access_token and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .accessToken(accessToken)
                .claimsPresence(JwtClaimName.ACCESS_TOKEN_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();

    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationCodeIdToken(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationCodeIdToken");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String code = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 4. Validate code and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .authorizationCode(code)
                .claimsPresence(JwtClaimName.CODE_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();

    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationCodeIdTokenUserBasicAuth(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationCodeIdTokenUserBasicAuth");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String code = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 4. Validate code and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .authorizationCode(code)
                .claimsPresence(JwtClaimName.CODE_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationTokenCode(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationTokenCode");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationTokenCodeUserBasicAuth(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationTokenCodeUserBasicAuth");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationTokenCodeIdToken(
            final String userId, final String userSecret,
            final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationTokenCodeIdToken");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String code = authorizationResponse.getCode();
        String accessToken = authorizationResponse.getAccessToken();
        String idToken = authorizationResponse.getIdToken();

        // 4. Validate access_token and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .authorizationCode(code)
                .accessToken(accessToken)
                .claimsPresence(JwtClaimName.CODE_HASH, JwtClaimName.ACCESS_TOKEN_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();

    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationTokenCodeIdTokenUserBasicAuth(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationTokenCodeIdTokenUserBasicAuth");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The accessToken is null");
        assertNotNull(authorizationResponse.getTokenType(), "The tokenType is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String code = authorizationResponse.getCode();
        String accessToken = authorizationResponse.getAccessToken();
        String idToken = authorizationResponse.getIdToken();

        // 4. Validate access_token and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .authorizationCode(code)
                .accessToken(accessToken)
                .claimsPresence(JwtClaimName.CODE_HASH, JwtClaimName.ACCESS_TOKEN_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdToken(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdToken");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenUserBasicAuth(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenUserBasicAuth");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationWithoutScope(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationWithoutScope");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = new ArrayList<String>(); // Empty scopes list
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationPromptNoneTrustedClient(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationPromptNoneTrustedClient");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

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

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setState(state);
        request.getPrompts().add(Prompt.NONE);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getCode(), "The code is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationPromptNoneFail(
            final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationPromptNoneFail");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setState(state);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationPromptLogin(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationPromptLogin");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.LOGIN);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationPromptConsent(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationPromptConsent");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.CONSENT);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationPromptConsentTrustedClient(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationPromptConsentTrustedClient");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

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

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.CONSENT);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationPromptLoginConsent(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationPromptLoginConsent");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.LOGIN);
        authorizationRequest.getPrompts().add(Prompt.CONSENT);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationPromptLoginConsentTrustedClient(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationPromptLoginConsentTrustedClient");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

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

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.LOGIN);
        authorizationRequest.getPrompts().add(Prompt.CONSENT);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationPromptNoneLoginConsentFail(
            final String userId, final String userSecret,
            final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationPromptLoginConsent");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setState(state);
        request.getPrompts().add(Prompt.NONE);
        request.getPrompts().add(Prompt.LOGIN);
        request.getPrompts().add(Prompt.CONSENT);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"redirectUri", "userId", "userSecret"})
    @Test
    public void requestAuthorizationCodeWithoutRedirectUri(
            final String redirectUri, final String userId, final String userSecret) throws Exception {
        showTitle("requestAuthorizationCodeWithoutRedirectUri");

        List<String> redirectUriList = Arrays.asList(redirectUri.split(StringUtils.SPACE));
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", redirectUriList);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"redirectUri", "userId", "userSecret"})
    @Test
    public void requestAuthorizationCodeWithoutRedirectUriUserBasicAuth(
            final String redirectUri, final String userId, final String userSecret) throws Exception {
        showTitle("requestAuthorizationCodeWithoutRedirectUriUserBasicAuth");

        List<String> redirectUriList = Arrays.asList(redirectUri.split(StringUtils.SPACE));
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app", redirectUriList);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, null, null);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"redirectUris", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationCodeWithoutRedirectUriFail(
            final String redirectUris, final String userId, final String userSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationCodeWithoutRedirectUriFail");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, null, null);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 400, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getErrorType(), "The error type is null");
        assertNotNull(authorizationResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationAccessToken(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationAccessToken");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest1.setState(state);
        authorizationRequest1.setNonce(nonce);

        AuthorizationResponse authorizationResponse1 = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest1, userId, userSecret);

        assertNotNull(authorizationResponse1.getLocation(), "The location is null");
        assertNotNull(authorizationResponse1.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse1.getState(), "The state is null");
        assertNotNull(authorizationResponse1.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse1.getScope(), "The scope must be null");

        String accessToken = authorizationResponse1.getAccessToken();

        // 4. Downstream client may be authorized by Jans Auth presenting the access token and client credentials
        responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        nonce = UUID.randomUUID().toString();
        state = UUID.randomUUID().toString();
/*
        AuthorizationRequest authorizationRequest2 = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest2.setAccessToken(accessToken);
        authorizationRequest2.setState(state);
        authorizationRequest2.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient2 = new AuthorizeClient(authorizationEndpoint);
        authorizeClient2.setRequest(authorizationRequest2);
        AuthorizationResponse authorizationResponse2 = authorizeClient2.exec();

        showClient(authorizeClient2);
        assertEquals(authorizationResponse2.getStatus(), 302, "Unexpected response code: " + authorizationResponse2.getStatus());
        assertNotNull(authorizationResponse2.getLocation(), "The location is null");
        assertNotNull(authorizationResponse2.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse2.getState(), "The state is null");
        assertNotNull(authorizationResponse2.getScope(), "The scope is null");

        String authorizationCode = authorizationResponse2.getCode();

        // 5. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse response3 = tokenClient1.exec();

        showClient(tokenClient1);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getEntity(), "The entity is null");
        assertNotNull(response3.getAccessToken(), "The access token is null");
        assertNotNull(response3.getExpiresIn(), "The expires in value is null");
        assertNotNull(response3.getTokenType(), "The token type is null");
        assertNotNull(response3.getRefreshToken(), "The refresh token is null");*/
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationAccessTokenUserBasicAuth(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationAccessTokenUserBasicAuth");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest1.setState(state);

        AuthorizationResponse authorizationResponse1 = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest1, userId, userSecret);

        assertNotNull(authorizationResponse1.getLocation(), "The location is null");
        assertNotNull(authorizationResponse1.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse1.getState(), "The state is null");
        assertNotNull(authorizationResponse1.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse1.getScope(), "The scope must be null");

        String accessToken = authorizationResponse1.getAccessToken();

        // 4. Downstream client may be authorized by Jans Auth presenting the access token and client credentials
        responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        nonce = UUID.randomUUID().toString();
        state = UUID.randomUUID().toString();
/*
        AuthorizationRequest authorizationRequest2 = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest2.setAccessToken(accessToken);
        authorizationRequest2.setState(state);
        authorizationRequest2.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient2 = new AuthorizeClient(authorizationEndpoint);
        authorizeClient2.setRequest(authorizationRequest2);
        AuthorizationResponse authorizationResponse2 = authorizeClient2.exec();

        showClient(authorizeClient2);
        assertEquals(authorizationResponse2.getStatus(), 302, "Unexpected response code: " + authorizationResponse2.getStatus());
        assertNotNull(authorizationResponse2.getLocation(), "The location is null");
        assertNotNull(authorizationResponse2.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse2.getState(), "The state is null");
        assertNotNull(authorizationResponse2.getScope(), "The scope is null");

        String authorizationCode = authorizationResponse2.getCode();

        // 5. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse response3 = tokenClient1.exec();

        showClient(tokenClient1);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getEntity(), "The entity is null");
        assertNotNull(response3.getAccessToken(), "The access token is null");
        assertNotNull(response3.getExpiresIn(), "The expires in value is null");
        assertNotNull(response3.getTokenType(), "The token type is null");
        assertNotNull(response3.getRefreshToken(), "The refresh token is null");*/
    }

    @Parameters({"redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationAccessTokenFail(
            final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationAccessTokenFail");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = null;
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setAccessToken("INVALID_ACCESS_TOKEN");
        request.setState(state);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationDenyAccessThenGrantAccess(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationDenyAccessThenGrantAccess");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String sessionId = null;

        // 2. Request authorization, authenticate resource owner and deny access
        {
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
            authorizationRequest.setState(state);
            authorizationRequest.setAcrValues(Arrays.asList("basic"));

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndDenyAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getErrorType());
            assertEquals(authorizationResponse.getErrorType(), AuthorizeErrorResponseType.ACCESS_DENIED);
            assertNotNull(authorizationResponse.getErrorDescription());
            assertNotNull(authorizationResponse.getState());

            sessionId = authorizationResponse.getSessionId();
        }

        // 3. Request authorization and deny access (resource owner is already authenticated)
        {
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
            authorizationRequest.setState(state);
            authorizationRequest.setSessionId(sessionId);
            authorizationRequest.setAcrValues(Arrays.asList("basic"));

            AuthorizationResponse authorizationResponse = authorizationRequestAndDenyAccess(
                    authorizationEndpoint, authorizationRequest);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getErrorType());
            assertEquals(authorizationResponse.getErrorType(), AuthorizeErrorResponseType.ACCESS_DENIED);
            assertNotNull(authorizationResponse.getErrorDescription());
            assertNotNull(authorizationResponse.getState());
        }

        // 4. Request authorization and grant access (resource owner is already authenticated)
        {
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
            authorizationRequest.setState(state);
            authorizationRequest.setSessionId(sessionId);
            authorizationRequest.setAcrValues(Arrays.asList("basic"));

            AuthorizationResponse authorizationResponse = authorizationRequestAndGrantAccess(
                    authorizationEndpoint, authorizationRequest);

            assertNotNull(authorizationResponse.getLocation(), "The location is null");
            assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
            assertNotNull(authorizationResponse.getState(), "The state is null");
            assertNotNull(authorizationResponse.getScope(), "The scope is null");
        }
    }

    /**
     * If a client has only openid scope and pairwise id, person should not have to authorize.
     */
    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    //@Test // Before run this test, set skipAuthorizationForOpenIdScopeAndPairwiseId = true
    public void requestAuthorizationForOpenIdScopeAndPairwiseId(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationForOpenIdScopeAndPairwiseId");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                authorizationEndpoint, authorizationRequest, userId, userSecret, false);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationUILocales(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationUILocales");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
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
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readClientResponse).ok().check();

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setNonce(nonce);
        authorizationRequest.setUiLocales(Arrays.asList("es"));

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationObjectUILocales(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationObjectUILocales");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setUiLocales(Arrays.asList("es"));

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"basic"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationAccessTokenSubjectTypePublic(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationAccessTokenSubjectTypePublic");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setResponseTypes(responseTypes);
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

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest1.setState(state);
        authorizationRequest1.setNonce(nonce);

        AuthorizationResponse authorizationResponse1 = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest1, userId, userSecret);

        assertNotNull(authorizationResponse1.getLocation(), "The location is null");
        assertNotNull(authorizationResponse1.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse1.getState(), "The state is null");
        assertNotNull(authorizationResponse1.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse1.getScope(), "The scope must be null");

        String idToken = authorizationResponse1.getIdToken();

        // 2. Validate access_token and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .claimsPresence(JwtClaimName.ACCESS_TOKEN_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();
    }
}
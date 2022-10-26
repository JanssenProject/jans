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
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class ResponseTypesRestrictionHttpTest extends BaseTest {

    /**
     * Registering without provide the response_types param, should register the Client using only
     * the <code>code</code> response type.
     */
    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void omittedResponseTypes(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("omittedResponseTypes");

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

    @DataProvider(name = "omittedResponseTypesFailDataProvider")
    public Object[][] omittedResponseTypesFailDataProvider(ITestContext context) {
        String redirectUris = context.getCurrentXmlTest().getParameter("redirectUris");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");
        String sectorIdentifierUri = context.getCurrentXmlTest().getParameter("sectorIdentifierUri");

        return new Object[][]{
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.ID_TOKEN), sectorIdentifierUri},
        };
    }

    /**
     * Authorization request with the other Response types combination should fail.
     */
    @Test(dataProvider = "omittedResponseTypesFailDataProvider")
    public void omittedResponseTypesFail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final List<ResponseType> responseTypes, final String sectorIdentifierUri) throws Exception {
        showTitle("omittedResponseTypesFail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
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
        assertTrue(authorizationResponse.getStatus() == 302
                || authorizationResponse.getStatus() == 400, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getErrorType(), "The error type is null");
        assertNotNull(authorizationResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Registering with the response_types param <code>code, id_token</code>.
     */
    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void responseTypesCodeIdToken(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("responseTypesCodeIdToken");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
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

        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS. APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 4. Validate code and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .authorizationCode(authorizationCode)
                .notNullAuthenticationTime()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        // 5. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse1)
                .notNullRefreshToken()
                .check();
    }

    @DataProvider(name = "responseTypesCodeIdTokenFailDataProvider")
    public Object[][] responseTypesCodeIdTokenFailDataProvider(ITestContext context) {
        String redirectUris = context.getCurrentXmlTest().getParameter("redirectUris");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String sectorIdentifierUri = context.getCurrentXmlTest().getParameter("sectorIdentifierUri");

        return new Object[][]{
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN), sectorIdentifierUri},
        };
    }

    /**
     * Authorization request with the other Response types combination should fail.
     */
    @Test(dataProvider = "responseTypesCodeIdTokenFailDataProvider")
    public void responseTypesCodeIdTokenFail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final List<ResponseType> responseTypes, final String sectorIdentifierUri) throws Exception {
        showTitle("responseTypesCodeIdTokenFail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN));
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
        assertTrue(authorizationResponse.getStatus() == 302
                || authorizationResponse.getStatus() == 400, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getErrorType(), "The error type is null");
        assertNotNull(authorizationResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void responseTypesTokenIdToken(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("responseTypesTokenIdToken");

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
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String accessToken = authorizationResponse.getAccessToken();
        String idToken = authorizationResponse.getIdToken();

        // 4. Validate code and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .accessToken(accessToken)
                .notNullAuthenticationTime()
                .notNullAccesTokenHash()
                .check();
    }

    @DataProvider(name = "responseTypesTokenIdTokenFailDataProvider")
    public Object[][] responseTypesTokenIdTokenFailDataProvider(ITestContext context) {
        String redirectUris = context.getCurrentXmlTest().getParameter("redirectUris");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String sectorIdentifierUri = context.getCurrentXmlTest().getParameter("sectorIdentifierUri");

        return new Object[][]{
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN), sectorIdentifierUri},
                {redirectUris, redirectUri, userId, userSecret, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN), sectorIdentifierUri},
        };
    }

    @Test(dataProvider = "responseTypesTokenIdTokenFailDataProvider")
    public void responseTypesTokenIdTokenFail(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final List<ResponseType> responseTypes, final String sectorIdentifierUri) throws Exception {
        showTitle("responseTypesTokenIdTokenFail");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN));
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
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertTrue(authorizationResponse.getStatus() == 302
                || authorizationResponse.getStatus() == 400, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getErrorType(), "The error type is null");
        assertNotNull(authorizationResponse.getErrorDescription(), "The error description is null");
    }
}
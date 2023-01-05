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
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
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
 * Test cases for the authorization code flow (HTTP)
 *
 * @author Javier Rojas Blum
 * @version May 14, 2019
 */
public class AuthorizationCodeFlowHttpTest extends BaseTest {

    /**
     * Test for the complete Authorization Code Flow.
     */
    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void authorizationCodeFlow(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = newTokenClient(tokenRequest);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(tokenResponse1)
                .notNullRefreshToken()
                .check();

        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .claimsPresence(JwtClaimName.CODE_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();

        // 5. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        tokenClient2.setExecutor(clientEngine(true));
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        AssertBuilder.tokenResponse(tokenResponse2)
                .notNullRefreshToken()
                .notNullScope()
                .check();
        String accessToken = tokenResponse2.getAccessToken();

        // 6. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setExecutor(clientEngine(true));
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.BIRTHDATE, JwtClaimName.GENDER, JwtClaimName.MIDDLE_NAME)
                .claimsPresence(JwtClaimName.NICKNAME, JwtClaimName.PREFERRED_USERNAME, JwtClaimName.PROFILE)
                .claimsPresence(JwtClaimName.WEBSITE, JwtClaimName.EMAIL_VERIFIED, JwtClaimName.PHONE_NUMBER)
                .claimsPresence(JwtClaimName.PHONE_NUMBER_VERIFIED, JwtClaimName.ADDRESS, JwtClaimName.USER_NAME)
                .claimsNoPresence("org_name", "work_phone")
                .check();
    }

    /**
     * Test for the complete Authorization Code Flow.
     * Register just the openid scope.
     * Request authorization with scopes openid, profile, address, email, phone, user_name.
     * Expected result is just prompt the user to authorize openid scope.
     */
    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void authorizationCodeFlowNegativeTest(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeFlowNegativeTest");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> registerScopes = Arrays.asList("openid");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, registerScopes, sectorIdentifierUri);

        assertTrue(registerResponse.getClaims().containsKey(SCOPE.toString()));
        assertNotNull(registerResponse.getClaims().get(SCOPE.toString()));
        assertEquals(registerResponse.getClaims().get(SCOPE.toString()), "openid");

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);
        assertEquals(authorizationResponse.getScope(), "openid");

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(tokenResponse1)
                .notNullRefreshToken()
                .check();

        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        // 5. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        AssertBuilder.tokenResponse(tokenResponse2)
                .notNullRefreshToken()
                .notNullScope()
                .check();
        assertEquals(tokenResponse2.getScope(), "openid");

        String accessToken = tokenResponse2.getAccessToken();

        // 6. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)

                .claimsNoPresence(JwtClaimName.NAME, JwtClaimName.BIRTHDATE, JwtClaimName.FAMILY_NAME, JwtClaimName.GENDER)
                .claimsNoPresence(JwtClaimName.GIVEN_NAME, JwtClaimName.MIDDLE_NAME, JwtClaimName.NICKNAME, JwtClaimName.PICTURE)
                .claimsNoPresence(JwtClaimName.PREFERRED_USERNAME, JwtClaimName.PROFILE, JwtClaimName.WEBSITE)
                .claimsNoPresence(JwtClaimName.EMAIL, JwtClaimName.EMAIL_VERIFIED, JwtClaimName.PHONE_NUMBER)
                .claimsNoPresence(JwtClaimName.PHONE_NUMBER_VERIFIED, JwtClaimName.ADDRESS, JwtClaimName.LOCALE)
                .claimsNoPresence(JwtClaimName.ZONEINFO, JwtClaimName.USER_NAME)
                .claimsNoPresence("org_name", "work_phone")
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void authorizationCodeWithNotAllowedScopeFlow(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeWithNotAllowedScopeFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        List<String> authorizationScopes = Arrays.asList("openid", "profile", "address", "email", "user_name", "mobile_phone");
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, authorizationScopes, clientId, nonce);

        String idToken = authorizationResponse.getIdToken();
        String authorizationCode = authorizationResponse.getCode();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .claimsPresence(JwtClaimName.CODE_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();

        // 4. Request access token
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

        String accessToken = tokenResponse.getAccessToken();

        // 5. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim("user_name"));
        assertNull(userInfoResponse.getClaim("phone_mobile_number"));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void authorizationCodeDynamicScopeFlow(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeDynamicScopeFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "user_name", "org_name", "work_phone");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String idToken = authorizationResponse.getIdToken();
        String authorizationCode = authorizationResponse.getCode();

        // 3. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        // 4. Request access token
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

        String accessToken = tokenResponse.getAccessToken();

        // 5. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim("user_name"));
        assertNotNull(userInfoResponse.getClaim("org_name"));
        assertNotNull(userInfoResponse.getClaim("work_phone"));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void authorizationCodeFlowWithOptionalNonce(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeFlowWithOptionalNonce");

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
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        String nonce = UUID.randomUUID().toString();
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(tokenResponse1)
                .notNullRefreshToken()
                .check();

        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .claimsPresence(JwtClaimName.NONCE, JwtClaimName.CODE_HASH)
                .check();
        assertEquals(jwt.getClaims().getClaimAsString(JwtClaimName.NONCE), nonce);

        // 5. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        AssertBuilder.tokenResponse(tokenResponse2)
                .notNullRefreshToken()
                .notNullScope()
                .check();
    }

    /**
     * When an authorization code is used more than once, all the tokens issued
     * for that authorization code must be revoked.
     */
    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void revokeTokens(final String userId, final String userSecret, final String redirectUris,
                             final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("revokeTokens");

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
        assertRegisterResponseClaimsNotNull(readClientResponse, RESPONSE_TYPES, REDIRECT_URIS.APPLICATION_TYPE, CLIENT_NAME, ID_TOKEN_SIGNED_RESPONSE_ALG, SCOPE);

        // 3. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNotNull(authorizationResponse.getIdToken(), "The id token is null");

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 4. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        // 5. Request access token using the authorization code.
        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        TokenResponse response2 = tokenClient1.execAuthorizationCode(authorizationCode, redirectUri,
                clientId, clientSecret);

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(response2)
                .notNullRefreshToken()
                .check();

        String accessToken = response2.getAccessToken();
        String refreshToken = response2.getRefreshToken();

        // 6. Request access token using the same authorization code one more time. This call must fail.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse response4 = tokenClient2.execAuthorizationCode(authorizationCode, redirectUri,
                clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(response4.getStatus(), 400, "Unexpected response code: " + response4.getStatus());
        assertNotNull(response4.getEntity(), "The entity is null");
        assertNotNull(response4.getErrorType(), "The error type is null");
        assertNotNull(response4.getErrorDescription(), "The error description is null");

        // 7. Request new access token using the refresh token. This call must fail too.
        TokenClient tokenClient3 = new TokenClient(tokenEndpoint);
        TokenResponse response5 = tokenClient3.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient3);
        assertEquals(response5.getStatus(), 400, "Unexpected response code: " + response5.getStatus());
        assertNotNull(response5.getEntity(), "The entity is null");
        assertNotNull(response5.getErrorType(), "The error type is null");
        assertNotNull(response5.getErrorDescription(), "The error description is null");

        // 8. Request user info should fail
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response7 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response7.getStatus(), 401, "Unexpected response code: " + response7.getStatus());
        assertNotNull(response7.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(response7.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void authorizationCodeFlowLoginHint(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeFlowLoginHint");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setLoginHint(userId);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret); // put userId explicitly, window.onload function result is not same as in browser (tested with chrome and FF)

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(tokenResponse1)
                .notNullRefreshToken()
                .check();

        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        // 5. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        AssertBuilder.tokenResponse(tokenResponse2)
                .notNullRefreshToken()
                .notNullScope()
                .check();

        String accessToken = tokenResponse2.getAccessToken();

        // 6. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim("user_name"));
        assertNull(userInfoResponse.getClaim("org_name"));
        assertNull(userInfoResponse.getClaim("work_phone"));
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        return authorizationResponse;
    }

    @Parameters({"userId", "userSecret", "redirectUri"})
    @Test(enabled = false)
    // retain claims script has to be enabled and client pre-configured (not avaiable in test suite)
    public void retainClaimAuthorizationCodeFlow(final String userId, final String userSecret, final String redirectUri) throws Exception {
        showTitle("authorizationCodeFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        String clientId = "0008-525a95a3-5fe1-4ecf-878c-06f438e3f500";
        String clientSecret = "V9RKUZOtfk92";//registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = newTokenClient(tokenRequest);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(tokenResponse1)
                .notNullRefreshToken()
                .check();

        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .claimsPresence(JwtClaimName.CODE_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .check();

        // 5. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        tokenClient2.setExecutor(clientEngine(true));
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        AssertBuilder.tokenResponse(tokenResponse2)
                .notNullRefreshToken()
                .notNullScope()
                .check();

        String accessToken = tokenResponse2.getAccessToken();
        System.out.println("AT2: " + accessToken);

        Jwt at2Jwt = Jwt.parse(accessToken);
        assertNotNull(at2Jwt, "AT2 is null");
        System.out.println("AT2 claims: " + at2Jwt.getClaims().toJsonString());
        assertEquals(at2Jwt.getClaims().getClaimAsString("claim1"), "value1");
    }
}
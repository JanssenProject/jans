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
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Functional tests for Sector Identifier URI Verification (HTTP)
 *
 * @author Javier Rojas Blum
 * @version May 7, 2019
 */
public class SectorIdentifierUrlVerificationHttpTest extends BaseTest {

    // Run this test with both pairwiseIdType persistent and algorithmic
    // And ensure shareSubjectIdBetweenClientsWithSameSectorId is set to false
    @Parameters({"redirectUris", "sectorIdentifierUri", "redirectUri", "userId", "userSecret"})
    @Test(enabled = false)
    public void pairwiseSectorIdentifierTypeToPreventSubjectIdentifierCorrelation(
            final String redirectUris, final String sectorIdentifierUri, final String redirectUri,
            final String userId, final String userSecret) throws Exception {
        showTitle("pairwiseSectorIdentifierTypeToPreventSubjectIdentifierCorrelation");

        RegisterResponse registerResponse1 = requestClientRegistration(redirectUris, sectorIdentifierUri);
        RegisterResponse registerResponse2 = requestClientRegistration(redirectUris, sectorIdentifierUri);

        String sub1 = requestAuthorizationCodeWithPairwiseSectorIdentifierType(redirectUri, userId, userSecret,
                registerResponse1.getClientId(),
                registerResponse1.getClientSecret(),
                registerResponse1.getResponseTypes());
        String sub2 = requestAuthorizationCodeWithPairwiseSectorIdentifierType(redirectUri, userId, userSecret,
                registerResponse2.getClientId(),
                registerResponse2.getClientSecret(),
                registerResponse2.getResponseTypes());

        assertNotEquals(sub1, sub2, "Each client must receive a different sub value");

        String sub3 = requestAuthorizationCodeWithPairwiseSectorIdentifierType(redirectUri, userId, userSecret,
                registerResponse1.getClientId(),
                registerResponse1.getClientSecret(),
                registerResponse1.getResponseTypes());
        String sub4 = requestAuthorizationCodeWithPairwiseSectorIdentifierType(redirectUri, userId, userSecret,
                registerResponse2.getClientId(),
                registerResponse2.getClientSecret(),
                registerResponse2.getResponseTypes());

        assertEquals(sub1, sub3, "Same client must receive the same sub value");
        assertEquals(sub2, sub4, "Same client must receive the same sub value");
    }

    // Run this test with both pairwiseIdType persistent and algorithmic
    // And ensure shareSubjectIdBetweenClientsWithSameSectorId is set to true
    @Parameters({"redirectUris", "sectorIdentifierUri", "redirectUri", "userId", "userSecret"})
    @Test(enabled = true)
    public void shareSubjectIdBetweenClientsWithSameSectorId(
            final String redirectUris, final String sectorIdentifierUri, final String redirectUri,
            final String userId, final String userSecret) throws Exception {
        showTitle("shareSubjectIdBetweenClientsWithSameSectorId");

        RegisterResponse registerResponse1 = requestClientRegistration(redirectUris, sectorIdentifierUri);
        RegisterResponse registerResponse2 = requestClientRegistration(redirectUris, sectorIdentifierUri);

        String sub1 = requestAuthorizationCodeWithPairwiseSectorIdentifierType(redirectUri, userId, userSecret,
                registerResponse1.getClientId(),
                registerResponse1.getClientSecret(),
                registerResponse1.getResponseTypes());
        String sub2 = requestAuthorizationCodeWithPairwiseSectorIdentifierType(redirectUri, userId, userSecret,
                registerResponse2.getClientId(),
                registerResponse2.getClientSecret(),
                registerResponse2.getResponseTypes());

        assertEquals(sub1, sub2, "Each client must share the same sub value");

        String sub3 = requestAuthorizationCodeWithPairwiseSectorIdentifierType(redirectUri, userId, userSecret,
                registerResponse1.getClientId(),
                registerResponse1.getClientSecret(),
                registerResponse1.getResponseTypes());
        String sub4 = requestAuthorizationCodeWithPairwiseSectorIdentifierType(redirectUri, userId, userSecret,
                registerResponse2.getClientId(),
                registerResponse2.getClientSecret(),
                registerResponse2.getResponseTypes());

        assertEquals(sub1, sub3, "Same client must receive the same sub value");
        assertEquals(sub2, sub4, "Same client must receive the same sub value");
    }

    public RegisterResponse requestClientRegistration(
            final String redirectUris, final String sectorIdentifierUri) {
        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        // Register client with Sector Identifier URL
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        return registerResponse;
    }

    public String requestAuthorizationCodeWithPairwiseSectorIdentifierType(
            final String redirectUri, final String userId, final String userSecret,
            final String clientId, final String clientSecret, final List<ResponseType> responseTypes) throws Exception {

        // 1. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
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
        assertEquals(authorizationResponse.getState(), state);

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 2. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        String sub = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);

        // 3. Request access token using the authorization code.
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

        String accessToken = tokenResponse.getAccessToken();

        // 4. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
        return sub;
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void publicSectorIdentifierType(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret) throws Exception {
        showTitle("publicSectorIdentifierType");

        String sub1 = requestAuthorizationCodeWithPublicSectorIdentifierType(redirectUris, redirectUri, userId, userSecret);
        String sub2 = requestAuthorizationCodeWithPublicSectorIdentifierType(redirectUris, redirectUri, userId, userSecret);

        assertEquals(sub1, sub2, "Each client must receive the same sub value");
    }

    public String requestAuthorizationCodeWithPublicSectorIdentifierType(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret) throws Exception {
        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        // 1. Register client with Sector Identifier URL
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
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
        assertEquals(authorizationResponse.getState(), state);

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        AssertBuilder.jwt(jwt)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .notNullAuthenticationTime()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        String sub = jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER);

        // 4. Request access token using the authorization code.
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

        String accessToken = tokenResponse.getAccessToken();

        // 5. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();

        return sub;
    }

    @Parameters({"redirectUris"})
    @Test
    public void sectorIdentifierUrlVerificationFail1(final String redirectUris) throws Exception {
        showTitle("sectorIdentifierUrlVerificationFail1");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri("https://INVALID_SECTOR_IDENTIFIER_URL");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).bad().check();
    }

    @Parameters({"sectorIdentifierUri"})
    @Test
    public void sectorIdentifierUrlVerificationFail2(final String sectorIdentifierUri) throws Exception {
        showTitle("sectorIdentifierUrlVerificationFail2");

        String redirectUris = "https://INVALID_REDIRECT_URI https://client.example.com/cb https://client.example.com/cb1 https://client.example.com/cb2";

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).bad().check();
    }

    /**
     * Register with pairwise Subject Type and without Sector Identifier URI must fail because there are multiple
     * hostnames in the Redirect URI list.
     */
    @Parameters({"redirectUris"})
    @Test
    public void sectorIdentifierUrlVerificationFail3(final String redirectUris) throws Exception {
        showTitle("sectorIdentifierUrlVerificationFail3");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(null);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).bad().check();
    }
}
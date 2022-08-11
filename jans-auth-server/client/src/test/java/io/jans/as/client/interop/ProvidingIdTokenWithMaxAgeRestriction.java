/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/**
 * OC5:FeatureTest-Providing ID Token with max age Restriction
 *
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class ProvidingIdTokenWithMaxAgeRestriction extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "clientJwksUri"})
    @Test
    public void providingIdTokenWithMaxAgeRestriction(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String clientJwksUri) throws Exception {
        showTitle("OC5:FeatureTest-Providing ID Token with max age Restriction");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setContacts(Arrays.asList("javier@gluu.org"));
        registerRequest.setGrantTypes(Arrays.asList(GrantType.AUTHORIZATION_CODE));
        registerRequest.setPostLogoutRedirectUris(StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setDefaultMaxAge(3600);
        registerRequest.setResponseTypes(responseTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        String sessionId;
        {
            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid");
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);
            AssertBuilder.authorizationResponse(authorizationResponse).check();
            assertEquals(authorizationResponse.getState(), state);

            String authorizationCode = authorizationResponse.getCode();
            sessionId = authorizationResponse.getSessionId();

            // 3. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            String idToken = tokenResponse.getIdToken();

            showClient(tokenClient);

            AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();

            // 4. Validate id_token
            AssertBuilder.jwtParse(idToken)
                    .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                    .notNullAccesTokenHash()
                    .notNullAuthenticationTime()
                    .check();
        }

        Thread.sleep(60000);

        {
            // 5. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
            authorizationRequest.setState(state);
            authorizationRequest.setMaxAge(30);
            authorizationRequest.setSessionId(sessionId);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);
            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
            assertEquals(authorizationResponse.getState(), state);

            String authorizationCode = authorizationResponse.getCode();

            // 6. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            String idToken = tokenResponse.getIdToken();

            showClient(tokenClient);
            AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();

            // 7. Validate id_token
            AssertBuilder.jwtParse(idToken)
                    .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                    .notNullAccesTokenHash()
                    .notNullAuthenticationTime()
                    .check();
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void providingIdTokenWithMaxAgeRestrictionJwtAuthorizationRequest(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Providing ID Token with max age Restriction (JWT Authorization Request)");

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

        String sessionId;
        {
            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);
            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();
            sessionId = authorizationResponse.getSessionId();

            // 3. Get Access Token
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

        Thread.sleep(60000);

        {
            // 4. Request authorization
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setSessionId(sessionId);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(30);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);

            AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                    authorizationEndpoint, authorizationRequest, userId, userSecret, false);
            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();

            // 5. Get Access Token
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
    }
}
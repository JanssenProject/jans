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
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Javier Rojas Blum
 * @version November 10, 2017
 */
public class PersistClientAuthorizationsHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void persistentClientAuthorizations(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("persistentClientAuthorizations");

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
        String clientSecret = registerResponse.getClientSecret();

        String sessionId = null;
        {
            // 2. Request authorization
            // Scopes: openid, profile
            // Authenticate user with login password then authorize.
            List<String> scopes = Arrays.asList("openid", "profile");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();
            sessionId = authorizationResponse.getSessionId();

            // 3. Request access token using the authorization code.
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

        {
            // 4. Request authorization
            // Scopes: openid, profile
            // Authenticate user with login password, do not show authorize page because those scopes are already authorized.
            List<String> scopes = Arrays.asList("openid", "profile");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                    authorizationEndpoint, authorizationRequest, userId, userSecret, false);

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();

            // 5. Request access token using the authorization code.
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

        {
            // 6. Request authorization
            // Scopes: openid, address, email
            // Authenticate user with login password then authorize.
            // It shows authorize page because those scopes are not yet authorized.
            List<String> scopes = Arrays.asList("openid", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret, false);

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();

            // 7. Request access token using the authorization code.
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

        {
            // 8. Request authorization
            // Scopes: openid, profile, address, email
            // Authenticate user with login password, do not show authorize page because those scopes are already authorized.
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                    authorizationEndpoint, authorizationRequest, userId, userSecret, false);

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();

            // 9. Request access token using the authorization code.
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

        {
            // 10. Request authorization
            // Scopes: openid, profile, address, email
            // Do not show authenticate page because we are including the session_id in the authorization request and the session is already authenticated.
            // Do not show authorize page because those scopes are already authorized.
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setSessionId(sessionId);

            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(authorizationRequest);

            AuthorizationResponse authorizationResponse = authorizeClient.exec();

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();

            // 11. Get Access Token
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

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void persistentClientAuthorizationsSameSession(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("persistentClientAuthorizationsSameSession");

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
        String clientSecret = registerResponse.getClientSecret();

        String sessionId = null;
        {
            // 2. Request authorization
            // Scopes: openid, profile
            // Authenticate user with login password then authorize.
            List<String> scopes = Arrays.asList("openid", "profile");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();
            sessionId = authorizationResponse.getSessionId();

            // 3. Request access token using the authorization code.
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

        {
            // 4. Request authorization
            // Scopes: openid, profile
            // Do not show authorize page because those scopes are already authorized.
            List<String> scopes = Arrays.asList("openid", "profile");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setSessionId(sessionId);

            AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                    authorizationEndpoint, authorizationRequest, null, null, false);

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();

            // 5. Request access token using the authorization code.
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

        {
            // 6. Request authorization
            // Scopes: openid, address, email
            // Shows authorize page because some scopes are not yet authorized.
            List<String> scopes = Arrays.asList("openid", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setSessionId(sessionId);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, null, null, false);

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();

            // 7. Request access token using the authorization code.
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

        {
            // 8. Request authorization
            // Scopes: openid, profile, address, email
            // Do not show authorize page because those scopes are already authorized.
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setSessionId(sessionId);

            AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                    authorizationEndpoint, authorizationRequest, null, null, false);

            AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

            String authorizationCode = authorizationResponse.getCode();

            // 9. Request access token using the authorization code.
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

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void doNotPersistAuthorizationWhenPreAuthorized(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("doNotPersistAuthorizationWhenPreAuthorized");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

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
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        // Scopes: openid, profile
        // Authenticate user with login password, do not show authorize page because the client is pre-authorized.
        List<String> scopes = Arrays.asList("openid", "profile");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                authorizationEndpoint, authorizationRequest, userId, userSecret, false);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Request access token using the authorization code.
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

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
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;



import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Javier Rojas Blum
 * @version March 9, 2019
 */
public class UserAuthenticationFilterHttpTest extends BaseTest {

    @Parameters({"redirectUris", "userInum", "userEmail", "sectorIdentifierUri"})
    @Test
    public void requestAccessTokenCustomAuth1(
            final String redirectUris, final String userInum, final String userEmail, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAccessTokenCustomAuth1");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setGrantTypes(grantTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.addCustomParameter("mail", userEmail);
        tokenRequest.addCustomParameter("inum", userInum);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse response1 = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
    }

    @Parameters({"redirectUris", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestAccessTokenCustomAuth2(
            final String redirectUris, final String userId, final String userSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAccessTokenCustomAuth2");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setGrantTypes(grantTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.addCustomParameter("uid", userId);
        tokenRequest.addCustomParameter("pwd", userSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse response1 = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
    }

    @Parameters({"redirectUris", "userInum", "userEmail", "sectorIdentifierUri"})
    @Test
    public void requestAccessTokenCustomAuth3(
            final String redirectUris, final String userInum, final String userEmail, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAccessTokenCustomAuth3");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setGrantTypes(grantTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.addCustomParameter("mail", userEmail);
        tokenRequest.addCustomParameter("inum", userInum);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse response1 = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestAccessTokenCustomAuth4(
            final String userId, final String userSecret, final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestAccessTokenCustomAuth4");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setGrantTypes(grantTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.addCustomParameter("uid", userId);
        tokenRequest.addCustomParameter("pwd", userSecret);
        tokenRequest.setAudience(tokenEndpoint);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setCryptoProvider(cryptoProvider);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse response1 = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
    }

    @Parameters({"redirectUris", "redirectUri", "userInum", "userEmail", "sectorIdentifierUri"})
    @Test
    public void requestAccessTokenCustomAuth5(
            final String redirectUris, final String redirectUri, final String userInum, final String userEmail,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAccessTokenCustomAuth5");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        // 1. Register client.
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
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.addCustomParameter("mail", userEmail);
        authorizationRequest.addCustomParameter("inum", userInum);
        authorizationRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

        AuthorizationResponse authorizationResponse = authorizationRequestAndGrantAccess(
                authorizationEndpoint, authorizationRequest);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse response2 = tokenClient1.exec();

        showClient(tokenClient1);
        assertEquals(response2.getStatus(), 200, "Unexpected response code: " + response2.getStatus());
        assertNotNull(response2.getEntity(), "The entity is null");
        assertNotNull(response2.getAccessToken(), "The access token is null");
        assertNotNull(response2.getExpiresIn(), "The expires in value is null");
        assertNotNull(response2.getTokenType(), "The token type is null");
    }
}
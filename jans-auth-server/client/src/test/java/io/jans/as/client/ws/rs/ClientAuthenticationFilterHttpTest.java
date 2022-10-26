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
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version September 3, 2018
 */
public class ClientAuthenticationFilterHttpTest extends BaseTest {

    private String clientId;
    private String customAttrValue1;

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestClientRegistrationWithCustomAttributes(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestClientRegistrationWithCustomAttributes");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        customAttrValue1 = UUID.randomUUID().toString();
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.addCustomAttribute("myCustomAttr1", customAttrValue1);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        clientId = response.getClientId();
    }

    @Parameters({"userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
    public void requestAccessTokenCustomClientAuth1(final String userId, final String userSecret,
                                                    final String redirectUri) throws Exception {
        showTitle("requestAccessTokenCustomClientAuth1");

        // 1. Request authorization and receive the authorization code.
        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

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
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 2. Validate code and id_token
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                .authorizationCode(authorizationCode)
                .notNullAuthenticationTime()
                .claimsPresence(JwtClaimName.CODE_HASH)
                .check();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.addCustomParameter("myCustomAttr1", customAttrValue1);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"userId", "userSecret"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
    public void requestAccessTokenCustomClientAuth2(final String userId, final String userSecret) throws Exception {
        showTitle("requestAccessTokenCustomClientAuth2");

        String username = userId;
        String password = userSecret;

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(username);
        tokenRequest.setPassword(password);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.addCustomParameter("myCustomAttr1", customAttrValue1);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }
}
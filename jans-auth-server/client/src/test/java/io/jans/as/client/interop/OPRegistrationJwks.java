/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.JwkResponse;
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
import io.jans.as.model.register.RegisterRequestParam;
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
 * @version October 4, 2017
 */
public class OPRegistrationJwks extends BaseTest {

    @Parameters({"redirectUri", "postLogoutRedirectUri", "clientJwksUri", "userId", "userSecret", "RS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void opRegistrationJwks(
            final String redirectUri, final String postLogoutRedirectUri, final String clientJwksUri,
            final String userId, final String userSecret, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("opRegistrationJwks");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<GrantType> grantTypes = Arrays.asList(GrantType.AUTHORIZATION_CODE);
        List<String> contacts = Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com");

        // 1. Register client
        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUri));
        registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setContacts(contacts);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwks(jwkResponse.getJwks().toString());

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        assertNotNull(registerResponse.getResponseTypes());
        assertTrue(registerResponse.getResponseTypes().containsAll(responseTypes));
        assertNotNull(registerResponse.getGrantTypes());
        assertTrue(registerResponse.getGrantTypes().containsAll(grantTypes));
        assertNotNull(registerResponse.getClaims().get(RegisterRequestParam.JWKS.getName()));
        assertNotNull(registerResponse.getClaims().get(RegisterRequestParam.TOKEN_ENDPOINT_AUTH_METHOD.getName()));
        assertEquals(AuthenticationMethod.PRIVATE_KEY_JWT.toString(), registerResponse.getClaims().get(RegisterRequestParam.TOKEN_ENDPOINT_AUTH_METHOD.getName()));

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getState());
        assertNotNull(authorizationResponse.getScope());

        String authorizationCode = authorizationResponse.getCode();

        // 3. Request access token using the authorization code.
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);

        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }

    @Parameters({"redirectUri", "postLogoutRedirectUri", "clientJwksUri", "userId", "userSecret", "RS256_keyId",
            "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void opRegistrationJwksUri(
            final String redirectUri, final String postLogoutRedirectUri, final String clientJwksUri,
            final String userId, final String userSecret, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("opRegistrationJwksUri");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<GrantType> grantTypes = Arrays.asList(GrantType.AUTHORIZATION_CODE);
        List<String> contacts = Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUri));
        registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setContacts(contacts);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        assertNotNull(registerResponse.getResponseTypes());
        assertTrue(registerResponse.getResponseTypes().containsAll(responseTypes));
        assertNotNull(registerResponse.getGrantTypes());
        assertTrue(registerResponse.getGrantTypes().containsAll(grantTypes));
        assertNotNull(registerResponse.getClaims().get(RegisterRequestParam.JWKS_URI.getName()));
        assertNotNull(registerResponse.getClaims().get(RegisterRequestParam.TOKEN_ENDPOINT_AUTH_METHOD.getName()));
        assertEquals(AuthenticationMethod.PRIVATE_KEY_JWT.toString(), registerResponse.getClaims().get(RegisterRequestParam.TOKEN_ENDPOINT_AUTH_METHOD.getName()));

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Request access token using the authorization code.
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);

        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();
    }
}

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
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version February 8, 2019
 */
public class TokenSignaturesHttpTest extends BaseTest {

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenNone(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenNone");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.NONE);
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
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getCode());
        assertNotNull(authorizationResponse.getState());
        assertNotNull(authorizationResponse.getScope());
        assertNull(authorizationResponse.getIdToken());

        String scope = authorizationResponse.getScope();
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
        assertEquals(tokenResponse.getStatus(), 200);
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getExpiresIn());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getRefreshToken());

        String idToken = tokenResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);

        AbstractCryptoProvider cryptoProvider = createCryptoProviderWithAllowedNone();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, null, SignatureAlgorithm.NONE);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenHS256(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, clientSecret, SignatureAlgorithm.HS256);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenHS384(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenHS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, clientSecret, SignatureAlgorithm.HS384);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenHS512(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), null,
                null, clientSecret, SignatureAlgorithm.HS512);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenRS256(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenRS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.RS256);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenRS384(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.RS384);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenRS512(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenRS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.RS512);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenES256(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenES256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.ES256);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenES384(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenES384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.ES384);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenES512(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenES512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.ES512);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenPS256(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenPS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.PS256);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenPRS384(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenPS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.PS384);
        assertTrue(validJwt);
    }

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationIdTokenPS512(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestAuthorizationIdTokenPS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.ID_TOKEN);

        // 1. Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setIdTokenSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

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
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String idToken = authorizationResponse.getIdToken();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        String keyId = jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        boolean validJwt = cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), keyId,
                jwkResponse.getJwks().toJSONObject(), null, SignatureAlgorithm.PS512);
        assertTrue(validJwt);
    }

    @Test
    public void printAlgorithmsAndProviders() {
        showTitle("printAlgorithmsAndProviders");

        JwtUtil.printAlgorithmsAndProviders();
    }

    @Test
    public void hs256() throws Exception {
        showTitle("hs256");

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";
        String secret = "071d68a5-9eb0-47fb-8608-f54a0d9c8ede";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        String encodedSignature = cryptoProvider.sign(signingInput, null, secret, SignatureAlgorithm.HS256);

        System.out.println("Encoded Signature: " + encodedSignature);
        assertEquals(encodedSignature, "BQwm1HCz0cjHYbulWMumkhZgyb2dD93uScXmC6Fv8Ik");

    }

    @Test
    public void hs384() throws Exception {
        showTitle("hs384");

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";
        String secret = "071d68a5-9eb0-47fb-8608-f54a0d9c8ede";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        String encodedSignature = cryptoProvider.sign(signingInput, null, secret, SignatureAlgorithm.HS384);

        System.out.println("Encoded Signature: " + encodedSignature);
        assertEquals(encodedSignature, "pe7gU1XxroqizSzucuHOor36L-M9_XPZ7KZcR6JW6xQAa2fmTLSDCc02fNER9atB");

    }

    @Test
    public void hs512() throws Exception {
        showTitle("hs512");

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";
        String secret = "071d68a5-9eb0-47fb-8608-f54a0d9c8ede";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();
        String encodedSignature = cryptoProvider.sign(signingInput, null, secret, SignatureAlgorithm.HS512);

        System.out.println("Encoded Signature: " + encodedSignature);
        assertEquals(encodedSignature, "IZsXiRrRfP9eNFj6snm_MGEnrtfvX8vOF43Z-FuFkRj29y0WUaPR50IXRDI5uGatJvVdr_i7eJCJ4N_EwwrIhQ");
    }

    @Parameters({"clientJwksUri", "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testRS256(final String clientJwksUri, final String keyId, final String dnName,
                          final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("Test RS256");

        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(signingInput, keyId, null, SignatureAlgorithm.RS256);

        System.out.println("Encoded Signature: " + encodedSignature);

        boolean signatureVerified = cryptoProvider.verifySignature(
                signingInput, encodedSignature, keyId, jwkResponse.getJwks().toJSONObject(), null,
                SignatureAlgorithm.RS256);
        assertTrue(signatureVerified, "Invalid signature");
    }

    @Parameters({"clientJwksUri", "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testRS384(final String clientJwksUri, final String keyId, final String dnName,
                          final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("Test RS384");

        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(signingInput, keyId, null, SignatureAlgorithm.RS384);

        System.out.println("Encoded Signature: " + encodedSignature);

        boolean signatureVerified = cryptoProvider.verifySignature(
                signingInput, encodedSignature, keyId, jwkResponse.getJwks().toJSONObject(), null,
                SignatureAlgorithm.RS384);
        assertTrue(signatureVerified, "Invalid signature");
    }

    @Parameters({"clientJwksUri", "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testRS512(final String clientJwksUri, final String keyId, final String dnName,
                          final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("Test RS512");

        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(signingInput, keyId, null, SignatureAlgorithm.RS512);

        System.out.println("Encoded Signature: " + encodedSignature);

        boolean signatureVerified = cryptoProvider.verifySignature(
                signingInput, encodedSignature, keyId, jwkResponse.getJwks().toJSONObject(), null,
                SignatureAlgorithm.RS512);
        assertTrue(signatureVerified, "Invalid signature");
    }

    @Parameters({"clientJwksUri", "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testES256(final String clientJwksUri, final String keyId, final String dnName,
                          final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("Test ES256");

        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(signingInput, keyId, null, SignatureAlgorithm.ES256);

        System.out.println("Encoded Signature: " + encodedSignature);

        boolean signatureVerified = cryptoProvider.verifySignature(
                signingInput, encodedSignature, keyId, jwkResponse.getJwks().toJSONObject(), null,
                SignatureAlgorithm.ES256);
        assertTrue(signatureVerified, "Invalid signature");

    }

    @Parameters({"clientJwksUri", "ES384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testES384(final String clientJwksUri, final String keyId, final String dnName,
                          final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("Test ES384");

        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(signingInput, keyId, null, SignatureAlgorithm.ES384);

        System.out.println("Encoded Signature: " + encodedSignature);

        boolean signatureVerified = cryptoProvider.verifySignature(
                signingInput, encodedSignature, keyId, jwkResponse.getJwks().toJSONObject(), null,
                SignatureAlgorithm.ES384);
        assertTrue(signatureVerified, "Invalid signature");
    }

    @Parameters({"clientJwksUri", "ES512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void testES512(final String clientJwksUri, final String keyId, final String dnName,
                          final String keyStoreFile, final String keyStoreSecret) throws Exception {
        showTitle("Test ES512");

        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse jwkResponse = jwkClient.exec();

        String signingInput = "eyJhbGciOiJIUzI1NiJ9.eyJub25jZSI6ICI2Qm9HN1QwR0RUZ2wiLCAiaWRfdG9rZW4iOiB7Im1heF9hZ2UiOiA4NjQwMH0sICJzdGF0ZSI6ICJTVEFURTAiLCAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vbG9jYWxob3N0L2NhbGxiYWNrMSIsICJ1c2VyaW5mbyI6IHsiY2xhaW1zIjogeyJuYW1lIjogbnVsbH19LCAiY2xpZW50X2lkIjogIkAhMTExMSEwMDA4IUU2NTQuQjQ2MCIsICJzY29wZSI6IFsib3BlbmlkIl0sICJyZXNwb25zZV90eXBlIjogWyJjb2RlIl19";

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        String encodedSignature = cryptoProvider.sign(signingInput, keyId, null, SignatureAlgorithm.ES512);

        System.out.println("Encoded Signature: " + encodedSignature);

        boolean signatureVerified = cryptoProvider.verifySignature(
                signingInput, encodedSignature, keyId, jwkResponse.getJwks().toJSONObject(), null,
                SignatureAlgorithm.ES512);
        assertTrue(signatureVerified, "Invalid signature");
    }

    @Test
    public void getMessageDigestSHA256() throws NoSuchProviderException, NoSuchAlgorithmException {
        showTitle("sha256");

        String input = "The quick brown fox jumps over the lazy dog";
        System.out.println("Input: " + input);

        byte[] digest = JwtUtil.getMessageDigestSHA256(input);

        BigInteger result = new BigInteger(1, digest);
        BigInteger expectedResult = new BigInteger("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592", 16);

        System.out.println("Result  : " + result);
        System.out.println("Expected: " + expectedResult);

        assertEquals(result, expectedResult);
    }

    @Test
    public void getMessageDigestSHA384() throws NoSuchProviderException, NoSuchAlgorithmException {
        showTitle("sha384");

        String input = "The quick brown fox jumps over the lazy dog";
        System.out.println("Input: " + input);

        byte[] digest = JwtUtil.getMessageDigestSHA384(input);

        BigInteger result = new BigInteger(1, digest);
        BigInteger expectedResult = new BigInteger("ca737f1014a48f4c0b6dd43cb177b0afd9e5169367544c494011e3317dbf9a509cb1e5dc1e85a941bbee3d7f2afbc9b1", 16);

        System.out.println("Result   : " + result);
        System.out.println("Expected : " + expectedResult);

        assertEquals(result, expectedResult);
    }

    @Test
    public void getMessageDigestSHA512() throws NoSuchProviderException, NoSuchAlgorithmException {
        showTitle("sha512");

        String input = "The quick brown fox jumps over the lazy dog";
        System.out.println("Input: " + input);

        byte[] digest = JwtUtil.getMessageDigestSHA512(input);

        BigInteger result = new BigInteger(1, digest);
        BigInteger expectedResult = new BigInteger("07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6", 16);

        System.out.println("Result   : " + result);
        System.out.println("Expected : " + expectedResult);

        assertEquals(result, expectedResult);
    }
}
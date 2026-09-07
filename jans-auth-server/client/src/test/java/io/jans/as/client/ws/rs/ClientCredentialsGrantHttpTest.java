/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseTest;
import io.jans.as.client.ClientInfoClient;
import io.jans.as.client.ClientInfoResponse;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TestCryptoContext;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.userinfo.UserInfoErrorResponseType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Javier Rojas Blum
 * @version February 8, 2019
 */
public class ClientCredentialsGrantHttpTest extends BaseTest {

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void defaultAuthenticationMethod(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("defaultAuthenticationMethod");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().notNullRegistrationClientUri().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .notNullScope()
                .nullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("name"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void defaultAuthenticationMethodFail(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("defaultAuthenticationMethodFail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretBasicAuthenticationMethod(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretBasicAuthenticationMethod");

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .notNullScope()
                .nullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("name"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");

        // 4. Request user info should fail
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 403);
        assertEquals(userInfoResponse.getErrorType(), UserInfoErrorResponseType.INSUFFICIENT_SCOPE);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretBasicAuthenticationMethodFail(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretBasicAuthenticationMethodFail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretPostAuthenticationMethod(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretPostAuthenticationMethod");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .notNullScope()
                .nullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("name"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretPostAuthenticationMethodFail1(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretPostAuthenticationMethodFail1");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretPostAuthenticationMethodFail2(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretPostAuthenticationMethodFail2");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(null);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretPostAuthenticationMethodFail3(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretPostAuthenticationMethodFail3");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(null);
        tokenRequest.setAuthPassword(null);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS256(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS256");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .notNullScope()
                .nullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("name"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS256Fail(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS256Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS384(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS384");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .notNullScope()
                .nullRefreshToken()
                .check();


        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("name"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS384Fail(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS384Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS512(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS512");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .notNullScope()
                .nullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("name"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS512Fail(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS512Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS256(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS256");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.RS256, SignatureAlgorithm.RS256);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS256Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS256Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.RS256, "RS256SIG_INVALID_KEYID");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS384(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS384");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.RS384, SignatureAlgorithm.RS384);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS384Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS384Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.RS384, "RS384SIG_INVALID_KEYID");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS512(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS512");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.RS512, SignatureAlgorithm.RS512);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS512Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS512Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.RS512, "RS512SIG_INVALID_KEYID");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES256(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES256");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.ES256, SignatureAlgorithm.ES256);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES256Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES256Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.ES256, "ES256SIG_INVALID_KEYID");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES384(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES384");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.ES384, SignatureAlgorithm.ES384);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES384Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES384Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.ES384, "ES384SIG_INVALID_KEYID");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES512(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES512");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.ES512, SignatureAlgorithm.ES512);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES512Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES512Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.ES512, "ES512SIG_INVALID_KEYID");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS256(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS256");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.PS256, SignatureAlgorithm.PS256);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS256Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS256Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.PS256, "PS256SIG_INVALID_KEYID");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS384(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS384");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.PS384, SignatureAlgorithm.PS384);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS384Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS384Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.PS384, "PS384SIG_INVALID_KEYID");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS512(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS512");
        privateKeyJwtAuthenticationMethod(redirectUris, sectorIdentifierUri, Algorithm.PS512, SignatureAlgorithm.PS512);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS512Fail(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS512Fail");
        privateKeyJwtAuthenticationMethodFail(redirectUris, sectorIdentifierUri, SignatureAlgorithm.PS512, "PS512SIG_INVALID_KEYID");
    }

    private void privateKeyJwtAuthenticationMethod(
            final String redirectUris, final String sectorIdentifierUri,
            final Algorithm algorithm, final SignatureAlgorithm signatureAlgorithm) throws Exception {
        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();
        String keyId = cryptoContext.getKeyId(algorithm);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(signatureAlgorithm);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .notNullScope()
                .nullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("name"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    private void privateKeyJwtAuthenticationMethodFail(
            final String redirectUris, final String sectorIdentifierUri,
            final SignatureAlgorithm signatureAlgorithm, final String invalidKeyId) throws Exception {
        List<String> scopes = Arrays.asList("clientinfo");

        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwks(cryptoContext.getJwksAsString());
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(signatureAlgorithm);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(invalidKeyId);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorType());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }
}
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

    @Parameters({"redirectUris", "clientJwksUri", "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS256(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS256");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS256Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS256Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("RS256SIG_INVALID_KEYID");
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

    @Parameters({"redirectUris", "clientJwksUri", "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS384(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS384");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS384Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS384Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("RS384SIG_INVALID_KEYID");
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

    @Parameters({"redirectUris", "clientJwksUri", "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS512(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS512");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS512Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS512Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("RS512SIG_INVALID_KEYID");
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

    @Parameters({"redirectUris", "clientJwksUri", "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES256(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES256");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES256Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES256Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("ES256SIG_INVALID_KEYID");
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

    @Parameters({"redirectUris", "clientJwksUri", "ES384_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES384(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES384");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES384Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES384Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("ES384SIG_INVALID_KEYID");
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

    @Parameters({"redirectUris", "clientJwksUri", "ES512_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES512(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES512");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES512Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES512Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("ES512SIG_INVALID_KEYID");
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

    @Parameters({"redirectUris", "clientJwksUri", "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS256(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS256");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS256);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS256Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS256Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS256);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("PS256SIG_INVALID_KEYID");
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

    @Parameters({"redirectUris", "clientJwksUri", "PS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS384(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS384");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS384);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS384Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS384Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS384);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("PS384SIG_INVALID_KEYID");
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

    @Parameters({"redirectUris", "clientJwksUri", "PS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS512(
            final String redirectUris, final String clientJwksUri, final String keyId, final String dnName,
            final String keyStoreFile, final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS512");

        List<String> scopes = Arrays.asList("clientinfo");
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.CLIENT_CREDENTIALS
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS512);
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

    @Parameters({"redirectUris", "clientJwksUri", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodPS512Fail(
            final String redirectUris, final String clientJwksUri, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodPS512Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScope(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.PS512);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId("PS512SIG_INVALID_KEYID");
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
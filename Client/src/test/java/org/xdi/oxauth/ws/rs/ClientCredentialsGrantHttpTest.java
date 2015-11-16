package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.token.TokenErrorResponseType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version November 16, 2015
 */
public class ClientCredentialsGrantHttpTest extends BaseTest {

    @Parameters({"redirectUris"})
    @Test
    public void defaultAuthenticationMethod(final String redirectUris) throws Exception {
        showTitle("defaultAuthenticationMethod");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris"})
    @Test
    public void defaultAuthenticationMethodFail(final String redirectUris) throws Exception {
        showTitle("defaultAuthenticationMethodFail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretBasicAuthenticationMethod(final String redirectUris) throws Exception {
        showTitle("clientSecretBasicAuthenticationMethod");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretBasicAuthenticationMethodFail(final String redirectUris) throws Exception {
        showTitle("clientSecretBasicAuthenticationMethodFail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretPostAuthenticationMethod(final String redirectUris) throws Exception {
        showTitle("clientSecretPostAuthenticationMethod");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretPostAuthenticationMethodFail1(final String redirectUris) throws Exception {
        showTitle("clientSecretPostAuthenticationMethodFail1");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretPostAuthenticationMethodFail2(final String redirectUris) throws Exception {
        showTitle("clientSecretPostAuthenticationMethodFail2");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretPostAuthenticationMethodFail3(final String redirectUris) throws Exception {
        showTitle("clientSecretPostAuthenticationMethodFail3");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS256(final String redirectUris) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS256");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS256);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS256Fail(final String redirectUris) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS256Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS256);
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

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS384(final String redirectUris) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS384");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS384);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS384Fail(final String redirectUris) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS384Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS384);
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

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS512(final String redirectUris) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS512");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris"})
    @Test
    public void clientSecretJwtAuthenticationMethodHS512Fail(final String redirectUris) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethodHS512Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword("INVALID_CLIENT_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.HS512);
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

    @Parameters({"redirectUris", "clientJwksUri", "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS256(final String redirectUris, final String clientJwksUri,
                                                       final String modulus, final String privateExponent) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS256");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS256SIG");
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "clientJwksUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS256Fail(final String redirectUris, final String clientJwksUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS256Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        final String modulus = "AL4LN5Ca4EKj1VDeFBy-ZnzC6LVmQHj8GH0ABpCvGEQ1BpFg5_XS9oTePogFyd0muNhHxW3LXiKGlzUhTR54jN1IDbksbBh_l0ff6NDuH8kjRfDhTuQW6IKORmM673dCzbTNlQe7khST3PdFZfZVdGfzWYYoGsoz-jMgj9gCM84McN_-QJ0xQWZGaetTSkK5z2YPm3zWMli3UshY71ot1FEEl0fcJysK5o-MBPtQj2PVFHG9q33J3gvZcQKCtYKJ1vwQP5TK22wiZQkHOYoRil02H7kIHYbm-EddAdHYXv2eSIsQPFs7Do30mbwM0no_D-5Mcw7rfx0oyMLsei6wa_0";
        final String privateExponent = "AK6vANQaiCi5D0rV1wbUvL_RKLYU1w5eKuQ7Mc2sJFINq4vV12FOGOronfHJ4FM3VJD457CUTmLN9A8SHSD1DgYYRQUAoBukrBmU5xukxfLMSW2wrCNcKzxWKrzrX1HwRcT7cxE4iH4BrApd7-sNgYJLXO7DzlwuiryUIaQb4iJyF1GR-POcXBX-x5NxFkcAJgR7zX4NuYxzxNvS0f0-JaHPIBEJf5iJ-ezdVsLwBNGQD2c_UsV-CA14IQiOs_hBIRZrI_AR-ORW0jFp8y2iU7PTaQT2vqONg9UckrGLSkbucv_YUOcuq-LV2ZNTbsSfE55fcVEDQR6_gk_goGnaxAE";
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS256SIG");
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

    @Parameters({"redirectUris", "clientJwksUri", "RS384_modulus", "RS384_privateExponent"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS384(final String redirectUris, final String clientJwksUri,
                                                       final String modulus, final String privateExponent) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS384");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS384SIG");
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "clientJwksUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS384Fail(final String redirectUris, final String clientJwksUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS384Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        final String modulus = "ALcRt8KqUO39zwkHl3MyX9HZ3Mv7I9LeGIsrsVxxboe3CIdk63HbyRLOIIopMzTfhvBs5zRAYOcY6FlWoe_81rDLnW6tnoAtqAeZnI0oxityYdry3HmQP9OCH9OL8jBjbJ3-kX5isjR0kGK3p233-oPkYX1Aj2jclegxOSbY0mvq45NVRKAhLQ3r56s-jPBQbYTGQKZ3FVHSQaKdXEk6xOnY8soXR5FACmVQk869EIGq6ysKpfFPyJOUNvlNm8tLwe5VAloFGjx8fCNxXLrcOPjqrTNoXjc7hy0InZMlk0ClmjiZtYM0xC2df4npm-bsZCc_44F1H-sCpvYt63vz-q8";
        final String privateExponent = "XZg_XNT6n1Jd4P3ynkCo4H8D9X2maQ6Hec-S0_JiUhxvzdj4zrNRb73WwQwjU-rb8FudMQehA0WmtNYn4Kxhju3qxUUafenZuFj-wuSPvHK0ON5lffkTyK0EXIF2BusuAvC9reIDvfHCR9YhUYWwnHHMrd6t8yyjr5xK3eOIYQdObagqSzksDR3ND0PUYJxbQyEHXyxVL4SHPVBpPu5DoctZk4pip2JN-4OY7XJlSCeB_AxtNgKPN6U4H3FsVogC3DRL2vHwsGnkPlwPOnNL7F-g7jYYM24fnegxDZ5mz7OobGZiZCLlbf3odhVNJxfczwuEBXr6pGVklRlEqIyOUQ";
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS384);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS384SIG");
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

    @Parameters({"redirectUris", "clientJwksUri", "RS512_modulus", "RS512_privateExponent"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS512(final String redirectUris, final String clientJwksUri,
                                                       final String modulus, final String privateExponent) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS512");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS512SIG");
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "clientJwksUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodRS512Fail(final String redirectUris, final String clientJwksUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodRS512Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        final String modulus = "AKfuT2zfMEb0XnruZif50OdvlQaxpJXyk8FBfJUY0DY6IsLDjo3eCvg93iAT6iZlvca5L7Frg5CBUXK_so6-7YsjkXMTGprYB6zrJGf1YTC9xex3BeC2WB5VIUNIqlU1Lpu4BM8p1KzWjkm2tDLmncpdcMjm8T4ztnFs814-Fq5qWKwCQHG4m1u35KUEXGfU2m2B1v0AX701n9EIKo8stfJj8Z3BVaFMUl8sECY_7ONWcj48UkGlv1maNsxQKdwGVkPsZNkOsCjp48bQ3zVKUz7eJae8R1R6Dq3vnk1Tt9iXQQRoADSkvFpoYpWM7Xk8s4fVAiMq6db2otUqZc_D0YU";
        final String privateExponent = "QKlnleFewoOH-cfgOBZeVS9G79vpJv_P2wMvSG3UhnzeM6Z_NqtACBQyeqGQcJaOe32FGsjuUO8qgIfF5mcoKoJYmDnL7cGvOusUCp-We-Em3AV8kulDhvJ6q2DIjaS7vKQf3fEafi7jfQjH3C2mpmxSaFlcnPnmj0hHcYtwyllyDbPReTk1gk4ONnhrUstCzA22HIMjEAX6c09kpYaO66J1KpepZ44W7de35sMxF3jKBc98ZV81YXqHXLIqfhXQN1SYAtiJ3TiV8V6dvsQcPaP8XhDOmcYuXvkNpvf_hRJY-r3eP1AS9-r2WarrXiwuKj2IuZ0AE2JRCDcNEzEMIQ";
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS512);
        tokenRequest.setRsaPrivateKey(privateKey);
        tokenRequest.setKeyId("RS512SIG");
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

    @Parameters({"redirectUris", "clientJwksUri", "ES256_d"})
    @Test
    public void privateKeyJwtAuthenticationMethodES256(final String redirectUris, final String clientJwksUri,
                                                       final String d) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES256");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setEcPrivateKey(privateKey);
        tokenRequest.setKeyId("ES256SIG");
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "clientJwksUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES256Fail(final String redirectUris, final String clientJwksUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES256Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        final String d = "AKbn5AHdXYg0SlpDW-rGxRjwrGpl4feIgI9c_O7Z1cuX";
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setEcPrivateKey(privateKey);
        tokenRequest.setKeyId("ES256SIG");
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

    @Parameters({"redirectUris", "clientJwksUri", "ES384_d"})
    @Test
    public void privateKeyJwtAuthenticationMethodES384(final String redirectUris, final String clientJwksUri,
                                                       final String d) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES384");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setEcPrivateKey(privateKey);
        tokenRequest.setKeyId("ES384SIG");
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "clientJwksUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES384Fail(final String redirectUris, final String clientJwksUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES384Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        final String d = "TzlwM3FzS6Ee2KWqt_fQqj7j-IAwuzD7ni4aqTcO-2-xs6CDJ-viwOC9O2BW1zsh";
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setEcPrivateKey(privateKey);
        tokenRequest.setKeyId("ES384SIG");
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

    @Parameters({"redirectUris", "clientJwksUri", "ES512_d"})
    @Test
    public void privateKeyJwtAuthenticationMethodES512(final String redirectUris, final String clientJwksUri,
                                                       final String d) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES512");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setEcPrivateKey(privateKey);
        tokenRequest.setKeyId("ES512SIG");
        tokenRequest.setAudience(tokenEndpoint);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getScope());
        assertNull(tokenResponse.getRefreshToken());

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "clientJwksUri"})
    @Test
    public void privateKeyJwtAuthenticationMethodES512Fail(final String redirectUris, final String clientJwksUri) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethodES512Fail");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(clientJwksUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant
        final String d = "Oj7lsmxkMK8NImsrYZIArqbGoIaT72BE2V8duzZtn41dyTV8thaIfWyU7iCesf-BIIVD4YQzBPOY46mWdc7uJuM";
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setEcPrivateKey(privateKey);
        tokenRequest.setKeyId("ES512SIG");
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

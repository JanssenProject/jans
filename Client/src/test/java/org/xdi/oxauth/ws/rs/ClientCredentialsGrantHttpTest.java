package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version November 8, 2015
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

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void clientSecretJwtAuthenticationMethod(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("clientSecretJwtAuthenticationMethod");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

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

    @Parameters({"redirectUris", "sectorIdentifierUri", "clientJwksUri", "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void privateKeyJwtAuthenticationMethod(final String redirectUris, final String sectorIdentifierUri,
                                                  final String clientJwksUri, final String modulus,
                                                  final String privateExponent) throws Exception {
        showTitle("privateKeyJwtAuthenticationMethod");

        List<String> scopes = Arrays.asList("clientinfo");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setScopes(scopes);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
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
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request Client Credentials Grant
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
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
}

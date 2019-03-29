package org.xdi.oxauth.interop;

import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.register.RegisterRequestParam;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

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

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getRegistrationClientUri());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());
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
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        assertNotNull(tokenResponse.getIdToken(), "The id token is null");
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
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getRegistrationClientUri());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());
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

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getState());
        assertNotNull(authorizationResponse.getScope());

        String authorizationCode = authorizationResponse.getCode();

        // 3. Request access token using the authorization code.
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        assertNotNull(tokenResponse.getIdToken(), "The id token is null");
    }
}

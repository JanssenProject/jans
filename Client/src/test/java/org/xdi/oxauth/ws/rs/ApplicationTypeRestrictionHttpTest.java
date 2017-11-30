/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class ApplicationTypeRestrictionHttpTest extends BaseTest {

    /**
     * Register a client without specify an Application Type.
     * Read client to check whether it is using the default Application Type <code>web</code>.
     */
    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void omittedApplicationType(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("omittedApplicationType");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(null, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
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

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertEquals(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()), ApplicationType.WEB.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get(SCOPE.toString()));
    }

    /**
     * Register a client with Application Type <code>web</code>.
     * Read client to check whether it is using the Application Type <code>web</code>.
     */
    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void applicationTypeWeb(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("applicationTypeWeb");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
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

        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        assertEquals(readClientResponse.getStatus(), 200, "Unexpected response code: " + readClientResponse.getEntity());
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertEquals(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()), ApplicationType.WEB.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get(SCOPE.toString()));
    }

    /**
     * Fail: Register a client with Application Type <code>web</code> and Redirect URI with the schema HTTP.
     */
    @Test
    public void applicationTypeWebFail1() throws Exception {
        showTitle("applicationTypeWebFail1");

        final String redirectUris = "http://client.example.com/cb";

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse registerResponse = registerClient.execRegister(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 400, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getEntity(), "The entity is null");
        assertNotNull(registerResponse.getErrorType(), "The error type is null");
        assertNotNull(registerResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Register a client with Application Type <code>native</code>.
     * Read client to check whether it is using the Application Type <code>native</code>.
     */
    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret"})
    @Test
    public void applicationTypeNativeSubjectTypePublic(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret) throws Exception {
        showTitle("applicationTypeNativeSubjectTypePublic");

        // 1. Register client
        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "user_name");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.NATIVE, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200);
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read 
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        assertEquals(readClientResponse.getStatus(), 200);
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertEquals(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()), ApplicationType.NATIVE.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get(SCOPE.toString()));

        // 3. Request authorization and receive the authorization code.
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

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 4. Request access token using the authorization code. 
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        assertEquals(tokenResponse1.getStatus(), 200);
        assertNotNull(tokenResponse1.getEntity());
        assertNotNull(tokenResponse1.getAccessToken());
        assertNotNull(tokenResponse1.getExpiresIn());
        assertNotNull(tokenResponse1.getTokenType());
        assertNotNull(tokenResponse1.getRefreshToken());

        String refreshToken = tokenResponse1.getRefreshToken();

        // 5. Validate id_token 
        Jwt jwt = Jwt.parse(idToken);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));

        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);

        assertTrue(rsaSigner.validate(jwt));

        // 6. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200);
        assertNotNull(tokenResponse2.getEntity());
        assertNotNull(tokenResponse2.getAccessToken());
        assertNotNull(tokenResponse2.getTokenType());
        assertNotNull(tokenResponse2.getRefreshToken());
        assertNotNull(tokenResponse2.getScope());

        String accessToken = tokenResponse2.getAccessToken();

        // 7. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200);
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void applicationTypeNativeSubjectTypePairwise(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String sectorIdentifierUri) throws Exception {
        showTitle("applicationTypeNativeSubjectTypePairwise");

        // 1. Register client
        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "user_name");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.NATIVE, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200);
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read 
        RegisterRequest readClientRequest = new RegisterRequest(registrationAccessToken);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readClientRequest);
        RegisterResponse readClientResponse = readClient.exec();

        showClient(readClient);
        assertEquals(readClientResponse.getStatus(), 200);
        assertNotNull(readClientResponse.getClientId());
        assertNotNull(readClientResponse.getClientSecret());
        assertNotNull(readClientResponse.getClientIdIssuedAt());
        assertNotNull(readClientResponse.getClientSecretExpiresAt());

        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertEquals(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()), ApplicationType.NATIVE.toString());
        assertNotNull(readClientResponse.getClaims().get(RESPONSE_TYPES.toString()));
        assertNotNull(readClientResponse.getClaims().get(REDIRECT_URIS.toString()));
        assertNotNull(readClientResponse.getClaims().get(APPLICATION_TYPE.toString()));
        assertNotNull(readClientResponse.getClaims().get(CLIENT_NAME.toString()));
        assertNotNull(readClientResponse.getClaims().get(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
        assertNotNull(readClientResponse.getClaims().get(SCOPE.toString()));

        // 3. Request authorization and receive the authorization code.
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

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 4. Request access token using the authorization code. 
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        assertEquals(tokenResponse1.getStatus(), 200);
        assertNotNull(tokenResponse1.getEntity());
        assertNotNull(tokenResponse1.getAccessToken());
        assertNotNull(tokenResponse1.getExpiresIn());
        assertNotNull(tokenResponse1.getTokenType());
        assertNotNull(tokenResponse1.getRefreshToken());

        String refreshToken = tokenResponse1.getRefreshToken();

        // 5. Validate id_token 
        Jwt jwt = Jwt.parse(idToken);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));

        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);

        assertTrue(rsaSigner.validate(jwt));

        // 6. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200);
        assertNotNull(tokenResponse2.getEntity());
        assertNotNull(tokenResponse2.getAccessToken());
        assertNotNull(tokenResponse2.getTokenType());
        assertNotNull(tokenResponse2.getRefreshToken());
        assertNotNull(tokenResponse2.getScope());

        String accessToken = tokenResponse2.getAccessToken();

        // 7. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200);
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
    }

    /**
     * Fail: Register a client with Application Type <code>native</code> and Redirect URI with the schema HTTPS.
     */
    @Test(enabled = false)
//allowed to register redirect_uris with custom schema to conform "OAuth 2.0 for Native Apps" spec
    public void applicationTypeNativeFail1() throws Exception {
        showTitle("applicationTypeNativeFail1");

        final String redirectUris = "https://client.example.com/cb";

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse registerResponse = registerClient.execRegister(ApplicationType.NATIVE, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 400, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getEntity(), "The entity is null");
        assertNotNull(registerResponse.getErrorType(), "The error type is null");
        assertNotNull(registerResponse.getErrorDescription(), "The error description is null");
    }

    /**
     * Fail: Register a client with Application Type <code>native</code> and Redirect URI with the host different than localhost.
     */
    @Parameters({"redirectUris"})
    @Test(enabled = false)
//allowed to register redirect_uris with custom schema to conform "OAuth 2.0 for Native Apps" spec
    public void applicationTypeNativeFail2(final String redirectUris) throws Exception {
        showTitle("applicationTypeNativeFail2");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse registerResponse = registerClient.execRegister(ApplicationType.NATIVE, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 400, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getEntity(), "The entity is null");
        assertNotNull(registerResponse.getErrorType(), "The error type is null");
        assertNotNull(registerResponse.getErrorDescription(), "The error description is null");
    }
}
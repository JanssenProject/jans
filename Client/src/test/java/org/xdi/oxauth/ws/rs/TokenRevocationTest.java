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
import org.xdi.oxauth.model.common.*;
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

/**
 * The oxAuth authorization server's revocation policy acts as follows:
 * The revocation of a particular token cause the revocation of related
 * tokens and the underlying authorization grant.  If the particular
 * token is a refresh token, then the authorization server will also
 * invalidate all access tokens based on the same authorization grant.
 * If the token passed to the request is an access token, the server will
 * revoke the respective refresh token as well.
 *
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
public class TokenRevocationTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestTokenRevocation1(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestTokenRevocation1");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest1 = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest1.setCode(authorizationCode);
        tokenRequest1.setRedirectUri(redirectUri);
        tokenRequest1.setAuthUsername(clientId);
        tokenRequest1.setAuthPassword(clientSecret);
        tokenRequest1.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest1);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");

        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
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

        // 5. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse2.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse2.getRefreshToken(), "The refresh token is null");
        assertNotNull(tokenResponse2.getScope(), "The scope is null");

        String accessToken2 = tokenResponse2.getAccessToken();
        String refreshToken2 = tokenResponse2.getRefreshToken();

        // 6. Request user info
        UserInfoClient userInfoClient1 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse1 = userInfoClient1.execUserInfo(accessToken2);

        showClient(userInfoClient1);
        assertEquals(userInfoResponse1.getStatus(), 200, "Unexpected response code: " + userInfoResponse1.getStatus());
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.BIRTHDATE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.GENDER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.MIDDLE_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.NICKNAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PREFERRED_USERNAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PROFILE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.WEBSITE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.EMAIL_VERIFIED));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PHONE_NUMBER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PHONE_NUMBER_VERIFIED));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.LOCALE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.USER_NAME));
        assertNull(userInfoResponse1.getClaim("org_name"));
        assertNull(userInfoResponse1.getClaim("work_phone"));

        // 7. Request refresh token revocation
        TokenRevocationRequest tokenRevocationRequest1 = new TokenRevocationRequest();
        tokenRevocationRequest1.setToken(refreshToken2);
        tokenRevocationRequest1.setTokenTypeHint(TokenTypeHint.REFRESH_TOKEN);
        tokenRevocationRequest1.setAuthUsername(clientId);
        tokenRevocationRequest1.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient1 = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient1.setRequest(tokenRevocationRequest1);

        TokenRevocationResponse tokenRevocationResponse1 = tokenRevocationClient1.exec();

        showClient(tokenRevocationClient1);
        assertEquals(tokenRevocationResponse1.getStatus(), 200, "Unexpected response code: " + tokenRevocationResponse1.getStatus());

        // 8. Request new access token using the revoked refresh token should fail.
        TokenClient tokenClient3 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse3 = tokenClient3.execRefreshToken(scope, refreshToken2, clientId, clientSecret);

        showClient(tokenClient3);
        assertEquals(tokenResponse3.getStatus(), 401, "Unexpected response code: " + tokenResponse3.getStatus());
        assertNotNull(tokenResponse3.getEntity(), "The entity is null");
        assertNotNull(tokenResponse3.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse3.getErrorDescription(), "The error description is null");

        // 9. Request token revocation
        TokenRevocationRequest tokenRevocationRequest2 = new TokenRevocationRequest();
        tokenRevocationRequest2.setToken(accessToken2);
        tokenRevocationRequest2.setTokenTypeHint(TokenTypeHint.ACCESS_TOKEN);
        tokenRevocationRequest2.setAuthUsername(clientId);
        tokenRevocationRequest2.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient2 = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient2.setRequest(tokenRevocationRequest2);

        TokenRevocationResponse tokenRevocationResponse2 = tokenRevocationClient2.exec();

        showClient(tokenRevocationClient2);
        assertEquals(tokenRevocationResponse2.getStatus(), 200, "Unexpected response code: " + tokenRevocationResponse2.getStatus());

        // 10. Request user info with the revoked access token should fail
        UserInfoClient userInfoClient2 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse2 = userInfoClient2.execUserInfo(accessToken2);

        showClient(userInfoClient2);
        assertEquals(userInfoResponse2.getStatus(), 400, "Unexpected response code: " + userInfoResponse2.getStatus());
        assertNotNull(userInfoResponse2.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(userInfoResponse2.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestTokenRevocation2(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestTokenRevocation2");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
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
        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");

        String accessToken = tokenResponse1.getAccessToken();
        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
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

        // 6. Request user info
        UserInfoClient userInfoClient1 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse1 = userInfoClient1.execUserInfo(accessToken);

        showClient(userInfoClient1);
        assertEquals(userInfoResponse1.getStatus(), 200, "Unexpected response code: " + userInfoResponse1.getStatus());
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.BIRTHDATE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.GENDER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.MIDDLE_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.NICKNAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PREFERRED_USERNAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PROFILE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.WEBSITE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.EMAIL_VERIFIED));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PHONE_NUMBER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PHONE_NUMBER_VERIFIED));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.LOCALE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.USER_NAME));
        assertNull(userInfoResponse1.getClaim("org_name"));
        assertNull(userInfoResponse1.getClaim("work_phone"));

        // 7. Request access token revocation
        TokenRevocationRequest tokenRevocationRequest2 = new TokenRevocationRequest();
        tokenRevocationRequest2.setToken(accessToken);
        tokenRevocationRequest2.setTokenTypeHint(TokenTypeHint.ACCESS_TOKEN);
        tokenRevocationRequest2.setAuthUsername(clientId);
        tokenRevocationRequest2.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient2 = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient2.setRequest(tokenRevocationRequest2);

        TokenRevocationResponse tokenRevocationResponse2 = tokenRevocationClient2.exec();

        showClient(tokenRevocationClient2);
        assertEquals(tokenRevocationResponse2.getStatus(), 200, "Unexpected response code: " + tokenRevocationResponse2.getStatus());

        // 8. Request user info with the revoked access token must fail
        UserInfoClient userInfoClient2 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse2 = userInfoClient2.execUserInfo(accessToken);

        showClient(userInfoClient2);
        assertEquals(userInfoResponse2.getStatus(), 400, "Unexpected response code: " + userInfoResponse2.getStatus());
        assertNotNull(userInfoResponse2.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(userInfoResponse2.getErrorDescription(), "Unexpected result: errorDescription not found");

        // 9. Request new access token using the refresh token must fail.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 401, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse2.getErrorDescription(), "The error description is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestTokenRevocation3(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestTokenRevocation3");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
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
        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");

        String accessToken = tokenResponse1.getAccessToken();
        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
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

        // 5. Request refresh token revocation
        TokenRevocationRequest tokenRevocationRequest1 = new TokenRevocationRequest();
        tokenRevocationRequest1.setToken(refreshToken);
        tokenRevocationRequest1.setTokenTypeHint(TokenTypeHint.REFRESH_TOKEN);
        tokenRevocationRequest1.setAuthUsername(clientId);
        tokenRevocationRequest1.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient1 = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient1.setRequest(tokenRevocationRequest1);

        TokenRevocationResponse tokenRevocationResponse1 = tokenRevocationClient1.exec();

        showClient(tokenRevocationClient1);
        assertEquals(tokenRevocationResponse1.getStatus(), 200, "Unexpected response code: " + tokenRevocationResponse1.getStatus());

        // 6. Request new access token using revoked refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 401, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse2.getErrorDescription(), "The error description is null");

        // 7. Request user info must fail
        UserInfoClient userInfoClient1 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse1 = userInfoClient1.execUserInfo(accessToken);

        showClient(userInfoClient1);
        assertEquals(userInfoResponse1.getStatus(), 400, "Unexpected response code: " + userInfoResponse1.getStatus());
        assertNotNull(userInfoResponse1.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(userInfoResponse1.getErrorDescription(), "Unexpected result: errorDescription not found");

        // 8. Request access token revocation
        TokenRevocationRequest tokenRevocationRequest2 = new TokenRevocationRequest();
        tokenRevocationRequest2.setToken(accessToken);
        tokenRevocationRequest2.setTokenTypeHint(TokenTypeHint.ACCESS_TOKEN);
        tokenRevocationRequest2.setAuthUsername(clientId);
        tokenRevocationRequest2.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient2 = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient2.setRequest(tokenRevocationRequest2);

        TokenRevocationResponse tokenRevocationResponse2 = tokenRevocationClient2.exec();

        showClient(tokenRevocationClient2);
        assertEquals(tokenRevocationResponse2.getStatus(), 200, "Unexpected response code: " + tokenRevocationResponse2.getStatus());

        // 9. Request user info with the revoked access token should fail
        UserInfoClient userInfoClient2 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse2 = userInfoClient2.execUserInfo(accessToken);

        showClient(userInfoClient2);
        assertEquals(userInfoResponse2.getStatus(), 400, "Unexpected response code: " + userInfoResponse2.getStatus());
        assertNotNull(userInfoResponse2.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(userInfoResponse2.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestTokenRevocationOptionalTokenTypeHint(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("requestTokenRevocationOptionalTokenTypeHint");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 3. Request access token using the authorization code.
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
        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");

        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
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

        // 5. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse2.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse2.getRefreshToken(), "The refresh token is null");
        assertNotNull(tokenResponse2.getScope(), "The scope is null");

        String accessToken = tokenResponse2.getAccessToken();
        String refreshToken2 = tokenResponse2.getRefreshToken();

        // 6. Request user info
        UserInfoClient userInfoClient1 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse1 = userInfoClient1.execUserInfo(accessToken);

        showClient(userInfoClient1);
        assertEquals(userInfoResponse1.getStatus(), 200, "Unexpected response code: " + userInfoResponse1.getStatus());
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.BIRTHDATE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.GENDER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.MIDDLE_NAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.NICKNAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PREFERRED_USERNAME));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PROFILE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.WEBSITE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.EMAIL_VERIFIED));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PHONE_NUMBER));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.PHONE_NUMBER_VERIFIED));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.LOCALE));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse1.getClaim(JwtClaimName.USER_NAME));
        assertNull(userInfoResponse1.getClaim("org_name"));
        assertNull(userInfoResponse1.getClaim("work_phone"));

        // 7. Request refresh token revocation
        TokenRevocationRequest tokenRevocationRequest1 = new TokenRevocationRequest();
        tokenRevocationRequest1.setToken(refreshToken2);
        tokenRevocationRequest1.setAuthUsername(clientId);
        tokenRevocationRequest1.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient1 = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient1.setRequest(tokenRevocationRequest1);

        TokenRevocationResponse tokenRevocationResponse1 = tokenRevocationClient1.exec();

        showClient(tokenRevocationClient1);
        assertEquals(tokenRevocationResponse1.getStatus(), 200, "Unexpected response code: " + tokenRevocationResponse1.getStatus());

        // 8. Request new access token using the revoked refresh token should fail.
        TokenClient tokenClient3 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse3 = tokenClient3.execRefreshToken(scope, refreshToken2, clientId, clientSecret);

        showClient(tokenClient3);
        assertEquals(tokenResponse3.getStatus(), 401, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse3.getEntity(), "The entity is null");
        assertNotNull(tokenResponse3.getErrorType(), "The error type is null");
        assertNotNull(tokenResponse3.getErrorDescription(), "The error description is null");

        // 9. Request token revocation
        TokenRevocationRequest tokenRevocationRequest2 = new TokenRevocationRequest();
        tokenRevocationRequest2.setToken(accessToken);
        tokenRevocationRequest2.setAuthUsername(clientId);
        tokenRevocationRequest2.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient2 = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient2.setRequest(tokenRevocationRequest2);

        TokenRevocationResponse tokenRevocationResponse2 = tokenRevocationClient2.exec();

        showClient(tokenRevocationClient2);
        assertEquals(tokenRevocationResponse2.getStatus(), 200, "Unexpected response code: " + tokenRevocationResponse2.getStatus());

        // 10. Request user info with the revoked access token should fail
        UserInfoClient userInfoClient2 = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse2 = userInfoClient2.execUserInfo(accessToken);

        showClient(userInfoClient2);
        assertEquals(userInfoResponse2.getStatus(), 400, "Unexpected response code: " + userInfoResponse2.getStatus());
        assertNotNull(userInfoResponse2.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(userInfoResponse2.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestTokenRevocationFail1(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        showTitle("requestTokenRevocationFail1");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");

        String refreshToken = tokenResponse.getRefreshToken();

        // 4. Request refresh token revocation
        TokenRevocationRequest tokenRevocationRequest = new TokenRevocationRequest();
        tokenRevocationRequest.setToken(refreshToken);
        tokenRevocationRequest.setTokenTypeHint(TokenTypeHint.REFRESH_TOKEN);
        tokenRevocationRequest.setAuthUsername(clientId);
        tokenRevocationRequest.setAuthPassword("INVALID_CLIENT_SECRET");

        TokenRevocationClient tokenRevocationClient = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient.setRequest(tokenRevocationRequest);

        TokenRevocationResponse tokenRevocationResponse = tokenRevocationClient.exec();

        showClient(tokenRevocationClient);
        assertEquals(tokenRevocationResponse.getStatus(), 401, "Unexpected response code: " + tokenRevocationResponse.getStatus());
        assertNotNull(tokenRevocationResponse.getEntity(), "The entity is null");
        assertNotNull(tokenRevocationResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenRevocationResponse.getErrorDescription(), "The error description is null");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestTokenRevocationFail2(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        showTitle("requestTokenRevocationFail2");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");

        String accessToken = tokenResponse.getAccessToken();

        // 4. Request refresh token revocation: Invalid tokens do not cause an error.
        TokenRevocationRequest tokenRevocationRequest = new TokenRevocationRequest();
        tokenRevocationRequest.setToken("INVALID_ACCESS_TOKEN");
        tokenRevocationRequest.setTokenTypeHint(TokenTypeHint.ACCESS_TOKEN);
        tokenRevocationRequest.setAuthUsername(clientId);
        tokenRevocationRequest.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient.setRequest(tokenRevocationRequest);

        TokenRevocationResponse tokenRevocationResponse = tokenRevocationClient.exec();

        showClient(tokenRevocationClient);
        assertEquals(tokenRevocationResponse.getStatus(), 200, "Unexpected response code: " + tokenRevocationResponse.getStatus());

        // 5. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.BIRTHDATE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GENDER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.MIDDLE_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NICKNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PREFERRED_USERNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PROFILE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.WEBSITE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.USER_NAME));
        assertNull(userInfoResponse.getClaim("org_name"));
        assertNull(userInfoResponse.getClaim("work_phone"));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestTokenRevocationFail3(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        showTitle("requestTokenRevocationFail3");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");

        String refreshToken = tokenResponse.getRefreshToken();

        // 4. Request token revocation without required parameter token
        TokenRevocationRequest tokenRevocationRequest = new TokenRevocationRequest();
        tokenRevocationRequest.setToken(null);
        tokenRevocationRequest.setAuthUsername(clientId);
        tokenRevocationRequest.setAuthPassword(clientSecret);

        TokenRevocationClient tokenRevocationClient = new TokenRevocationClient(tokenRevocationEndpoint);
        tokenRevocationClient.setRequest(tokenRevocationRequest);

        TokenRevocationResponse tokenRevocationResponse = tokenRevocationClient.exec();

        showClient(tokenRevocationClient);
        assertEquals(tokenRevocationResponse.getStatus(), 400, "Unexpected response code: " + tokenRevocationResponse.getStatus());
        assertNotNull(tokenRevocationResponse.getEntity(), "The entity is null");
        assertNotNull(tokenRevocationResponse.getErrorType(), "The error type is null");
        assertNotNull(tokenRevocationResponse.getErrorDescription(), "The error description is null");
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        return authorizationResponse;
    }

    private RegisterResponse registerClient(
            final String redirectUris, List<ResponseType> responseTypes, List<String> scopes, String sectorIdentifierUri) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
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
        return registerResponse;
    }
}

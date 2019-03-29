/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jws.RSASigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version March 14, 2019
 */
public class ValidateIdTokenHashesTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void validateIdTokenHashes(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
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

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        String stateParam = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(stateParam);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertEquals(authorizationResponse.getState(), stateParam);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String accessToken = authorizationResponse.getAccessToken();
        String idToken = authorizationResponse.getIdToken();
        String state = authorizationResponse.getState();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));

        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);

        assertTrue(rsaSigner.validate(jwt));

        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
        assertTrue(rsaSigner.validateAuthorizationCode(authorizationCode, jwt));

        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
        assertTrue(rsaSigner.validateAccessToken(accessToken, jwt));

        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.STATE_HASH));
        assertTrue(rsaSigner.validateState(state, jwt));

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
        assertEquals(tokenResponse1.getStatus(), 200, "Unexpected response code: " + tokenResponse1.getStatus());
        assertNotNull(tokenResponse1.getEntity(), "The entity is null");
        assertNotNull(tokenResponse1.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse1.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse1.getRefreshToken(), "The refresh token is null");

        String refreshToken = tokenResponse1.getRefreshToken();
        String idToken2 = tokenResponse1.getIdToken();
        String accessToken2 = tokenResponse1.getAccessToken();

        // 5. Validate id_token
        Jwt jwt2 = Jwt.parse(idToken2);
        assertNotNull(jwt2.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt2.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));

        RSAPublicKey publicKey2 = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt2.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner2 = new RSASigner(SignatureAlgorithm.RS256, publicKey2);

        assertTrue(rsaSigner2.validate(jwt2));

        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
        assertTrue(rsaSigner2.validateAccessToken(accessToken2, jwt2));

        assertNull(jwt2.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
        assertNull(jwt2.getClaims().getClaimAsString(JwtClaimName.STATE_HASH));

        // 6. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse2.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse2.getRefreshToken(), "The refresh token is null");
        assertNotNull(tokenResponse2.getScope(), "The scope is null");

        String accessToken3 = tokenResponse2.getAccessToken();

        // 7. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken3);

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
}

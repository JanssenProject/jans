/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertRegisterResponseOk(registerResponse, 201, true);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        String stateParam = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(stateParam);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);
        assertAuthorizationResponse(authorizationResponse, true);
        assertEquals(authorizationResponse.getState(), stateParam);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String accessToken = authorizationResponse.getAccessToken();
        String idToken = authorizationResponse.getIdToken();
        String state = authorizationResponse.getState();

        // 3. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        assertIdToken(jwt);

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
        assertTokenResponseOk(tokenResponse1, true, false);

        String refreshToken = tokenResponse1.getRefreshToken();
        String idToken2 = tokenResponse1.getIdToken();
        String accessToken2 = tokenResponse1.getAccessToken();

        // 5. Validate id_token
        Jwt jwt2 = Jwt.parse(idToken2);
        assertIdToken(jwt2);

        RSAPublicKey publicKey2 = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt2.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner2 = new RSASigner(SignatureAlgorithm.RS256, publicKey2);

        assertTrue(rsaSigner2.validate(jwt2));

        assertNotNull(jwt2.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
        assertTrue(rsaSigner2.validateAccessToken(accessToken2, jwt2));

        assertNull(jwt2.getClaims().getClaimAsString(JwtClaimName.STATE_HASH));

        // 6. Request new access token using the refresh token.
        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        TokenResponse tokenResponse2 = tokenClient2.execRefreshToken(scope, refreshToken, clientId, clientSecret);

        showClient(tokenClient2);
        assertTokenResponseOk(tokenResponse2, true, false);
        assertNotNull(tokenResponse2.getScope(), "The scope is null");

        String accessToken3 = tokenResponse2.getAccessToken();

        // 7. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken3);

        showClient(userInfoClient);
        assertUserInfoBasicMinimumResponseOk(userInfoResponse, 200);
        assertUserInfoPersonalDataNotNull(userInfoResponse);        
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.BIRTHDATE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GENDER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.MIDDLE_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NICKNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PREFERRED_USERNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PROFILE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.WEBSITE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.USER_NAME));
        assertNull(userInfoResponse.getClaim("org_name"));
        assertNull(userInfoResponse.getClaim("work_phone"));
    }
}

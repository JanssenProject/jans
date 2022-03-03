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
import io.jans.as.client.client.AssertBuilder;
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
import io.jans.as.model.util.Util;
import org.apache.commons.codec.binary.Base64;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * http://tools.ietf.org/html/rfc2617#section-2
 *
 * @author Javier Rojas Blum
 * @version January 26. 2018
 */
public class ClientSecretBasicTest extends BaseTest {

    @Test
    public void testEncode1() {
        showTitle("testEncode1");

        String clientId = "Aladdin";
        String clientSecret = "open sesame";
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        assertEquals(tokenRequest.getEncodedCredentials(), "QWxhZGRpbjpvcGVuK3Nlc2FtZQ==");
    }

    @Test
    public void testEncode2() {
        showTitle("testEncode2");

        String clientId = "a+b";
        String clientSecret = "c+d";
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        assertEquals(tokenRequest.getEncodedCredentials(), "YSUyQmI6YyUyQmQ=");
    }

    @Test
    public void testEncode3() {
        showTitle("testEncode3");

        String clientId = "@!12AD!0008!6D30.23D7";
        String clientSecret = "P@55W0rd!";
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        assertEquals(tokenRequest.getEncodedCredentials(), "JTQwJTIxMTJBRCUyMTAwMDglMjE2RDMwLjIzRDc6UCU0MDU1VzByZCUyMQ==");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void authorizationCodeFlow(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("authorizationCodeFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
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
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponseBuilder(authorizationResponse).notNullScope().notNullState().checkAsserts();

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

        String expectedEncodedCredentials = Base64.encodeBase64String(Util.getBytes(
                URLEncoder.encode(clientId, Util.UTF8_STRING_ENCODING)
                        + ":"
                        + URLEncoder.encode(clientSecret, Util.UTF8_STRING_ENCODING)));
        assertEquals(tokenRequest.getEncodedCredentials(), expectedEncodedCredentials);

        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        assertTokenResponseOk(tokenResponse1, true, false);

        String refreshToken = tokenResponse1.getRefreshToken();

        // 4. Validate id_token
        Jwt jwt = Jwt.parse(idToken);
        assertJwtStandarClaimsNotNull(jwt, false);
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
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
        assertTokenResponseOk(tokenResponse2, true, false);
        assertNotNull(tokenResponse2.getScope(), "The scope is null");

        String accessToken = tokenResponse2.getAccessToken();

        // 6. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertUserInfoBasicMinimumResponseOk(userInfoResponse, 200);
        assertUserInfoPersonalDataNotNull(userInfoResponse);
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PHONE_NUMBER_VERIFIED));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.USER_NAME));
        assertNull(userInfoResponse.getClaim("org_name"));
        assertNull(userInfoResponse.getClaim("work_phone"));
    }
}

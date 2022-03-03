/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.jwt.JwtClaimName;
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

/**
 * OC5:FeatureTest-Support scope Requesting profile Claims
 *
 * @author Javier Rojas Blum
 * @version October 14, 2019
 */
public class SupportScopeRequestingProfileClaims extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void supportScopeRequestingProfileClaims(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Support scope Requesting profile Claims");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertRegisterResponseOk(registerResponse, 201, true);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponseBuilder(authorizationResponse).notNullScope().notNullState().checkAsserts();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Get Access Token
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponseBuilder(tokenResponse)
                .notNullRefreshToken()
                .checkAsserts();

        String accessToken = tokenResponse.getAccessToken();

        // 4. Request user info
        UserInfoResponse userInfoResponse = requestUserInfo(accessToken);
        assertUserInfoBasicMinimumResponseOk(userInfoResponse, 200);
        assertUserInfoPersonalDataNotNull(userInfoResponse, false);
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.WEBSITE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.BIRTHDATE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GENDER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PROFILE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PREFERRED_USERNAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.MIDDLE_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.UPDATED_AT));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NICKNAME));
    }
}
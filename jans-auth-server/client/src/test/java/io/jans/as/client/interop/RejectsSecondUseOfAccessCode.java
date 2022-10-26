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
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
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
 * OC5:FeatureTest-Rejects Second Use of Access Code
 *
 * @author Javier Rojas Blum
 * @version November 3, 2016
 */
public class RejectsSecondUseOfAccessCode extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void rejectsSecondUseOfAccessCode(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Rejects Second Use of Access Code");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        assertNotNull(authorizationResponse.getIdToken(), "The id token is null");

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();

        // 3. Request access token using the authorization code.
        String accessToken;
        String refreshToken;
        {
            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            TokenResponse tokenResponse = tokenClient.execAuthorizationCode(authorizationCode, redirectUri,
                    clientId, clientSecret);

            showClient(tokenClient);
            AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();

            accessToken = tokenResponse.getAccessToken();
            refreshToken = tokenResponse.getRefreshToken();
        }

        // 4. Request user info
        {
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(userInfoResponse)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL)
                    .check();
        }

        // 5. Request access token using the same authorization code one more time. This call must fail.
        {
            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            TokenResponse tokenResponse = tokenClient.execAuthorizationCode(authorizationCode, redirectUri, clientId, clientSecret);

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 400, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getErrorType(), "The error type is null");
            assertNotNull(tokenResponse.getErrorDescription(), "The error description is null");
        }
    }
}
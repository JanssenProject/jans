/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.load.benchmark;

import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.load.benchmark.suite.BenchmarkTestListener;
import org.xdi.oxauth.load.benchmark.suite.BenchmarkTestSuiteListener;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Movchan
 * @version November 29, 2017
 */

@Listeners({BenchmarkTestSuiteListener.class, BenchmarkTestListener.class})
public class BenchmarkRequestAccessToken extends BaseTest {

    private String clientId;
    private String clientSecret;

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @BeforeClass
    public void registerClient(final String userId, final String userSecret, String redirectUris, String sectorIdentifierUri) throws Exception {
        Reporter.log("Register client", true);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "user_name");

        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes, sectorIdentifierUri);

        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        this.clientId = registerResponse.getClientId();
        this.clientSecret = registerResponse.getClientSecret();
    }

    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 200, threadPoolSize = 1)
    public void requestAccessTokenPassword1(final String userId, final String userSecret) throws Exception {
        requestAccessTokenPassword(userId, userSecret, this.clientId, this.clientSecret);
    }

    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 200, threadPoolSize = 5, dependsOnMethods = {"requestAccessTokenPassword1"})
    public void requestAccessTokenPassword2(final String userId, final String userSecret) throws Exception {
        requestAccessTokenPassword(userId, userSecret, this.clientId, this.clientSecret);
    }

    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 200, threadPoolSize = 2, dependsOnMethods = {"requestAccessTokenPassword2"})
    public void requestAccessTokenPassword4(final String userId, final String userSecret) throws Exception {
        requestAccessTokenPassword(userId, userSecret, this.clientId, this.clientSecret);
    }

    private void requestAccessTokenPassword(final String userId, final String userSecret, String clientId, String clientSecret) throws Exception {
        // Request Resource Owner Credentials Grant
        String scope = "openid";

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        TokenResponse response1 = tokenClient.execResourceOwnerPasswordCredentialsGrant(userId, userSecret, scope, clientId, clientSecret);

        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getRefreshToken(), "The refresh token is null");
        assertNotNull(response1.getScope(), "The scope is null");
        assertNotNull(response1.getIdToken(), "The id token is null");
    }

    private RegisterResponse registerClient(
            final String redirectUris, List<ResponseType> responseTypes, List<String> scopes, String sectorIdentifierUri) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth benchmark test app",
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

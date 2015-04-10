/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.load.benchmark;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.load.benchmark.suite.BenchmarkTestListener;
import org.xdi.oxauth.load.benchmark.suite.BenchmarkTestSuiteListener;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * @author Yuriy Movchan
 * @version 0.1, 04/10/2015
 */

@Listeners({BenchmarkTestSuiteListener.class, BenchmarkTestListener.class })
public class BenchmarkRequestAccessToken extends BaseTest {

    private String clientId;
	private String clientSecret;

	@Parameters({"userId", "userSecret", "redirectUris"})
    @BeforeClass
    public void registerClient(final String userId, final String userSecret, String redirectUris) throws Exception {
        Reporter.log("Register client", true);

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth benchmark test app", StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

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
    @Test(invocationCount = 1000, threadPoolSize = 10)
    public void requestAccessTokenPassword1(final String userId, final String userSecret) throws Exception {
    	requestAccessTokenPassword(userId, userSecret, this.clientId, this.clientSecret);
    }

    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 1000, threadPoolSize = 10, dependsOnMethods = { "requestAccessTokenPassword1" })
    public void requestAccessTokenPassword2(final String userId, final String userSecret) throws Exception {
    	requestAccessTokenPassword(userId, userSecret, this.clientId, this.clientSecret);
    }

    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 500, threadPoolSize = 2, dependsOnMethods = { "requestAccessTokenPassword2" })
    public void requestAccessTokenPassword3(final String userId, final String userSecret) throws Exception {
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

}

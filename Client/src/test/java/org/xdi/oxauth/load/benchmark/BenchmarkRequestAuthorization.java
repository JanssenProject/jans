/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.load.benchmark;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.load.benchmark.suite.BenchmarkTestListener;
import org.xdi.oxauth.load.benchmark.suite.BenchmarkTestSuiteListener;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * @author Yuriy Movchan
 * @version 0.1, 04/10/2015
 */

@Listeners({BenchmarkTestSuiteListener.class, BenchmarkTestListener.class })
public class BenchmarkRequestAuthorization extends BaseTest {

    private String clientId;
	private String redirectUri;

	@Parameters({"userId", "userSecret", "redirectUris"})
    @BeforeClass
    public void registerClient(final String userId, final String userSecret, String redirectUris) throws Exception {
        Reporter.log("Register client", true);

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        List<String> redirectUrisList = StringUtils.spaceSeparatedToList(redirectUris);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth benchmark test app",
        		redirectUrisList);
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
        this.redirectUri = redirectUrisList.get(0);
    }


    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 1000, threadPoolSize = 10)
    public void testAuthorization1(final String userId, final String userSecret) throws Exception {
        testAuthorizationImpl(userId, userSecret, this.redirectUri, this.clientId);
    }

    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 1000, threadPoolSize = 10, dependsOnMethods = { "testAuthorization1" })
    public void testAuthorization2(final String userId, final String userSecret) throws Exception {
        testAuthorizationImpl(userId, userSecret, this.redirectUri, this.clientId);
    }

    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 500, threadPoolSize = 2, dependsOnMethods = { "testAuthorization2" })
    public void testAuthorization3(final String userId, final String userSecret) throws Exception {
        testAuthorizationImpl(userId, userSecret, this.redirectUri, this.clientId);
    }

	private void testAuthorizationImpl(final String userId, final String userSecret, String redirectUri, String clientId) {
		final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);
        responseTypes.add(ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = "af0ifjsldkj";
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setState(state);
        request.setNonce(nonce);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(this.authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        assertNotNull(response.getCode(), "The authorization code is null");
        assertNotNull(response.getAccessToken(), "The access token is null");
        assertNotNull(response.getState(), "The state is null");
        assertNotNull(response.getTokenType(), "The token type is null");
        assertNotNull(response.getExpiresIn(), "The expires in value is null");
        assertNotNull(response.getScope(), "The scope must be null");
	}

}

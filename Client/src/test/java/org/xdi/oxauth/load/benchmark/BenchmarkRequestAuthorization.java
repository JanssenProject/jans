/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.load.benchmark;

import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.load.benchmark.suite.BenchmarkTestListener;
import org.xdi.oxauth.load.benchmark.suite.BenchmarkTestSuiteListener;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version June 19, 2015
 */

@Listeners({BenchmarkTestSuiteListener.class, BenchmarkTestListener.class})
public class BenchmarkRequestAuthorization extends BaseTest {

    private String clientId;
    private String redirectUri;

    @Parameters({"userId", "userSecret", "redirectUris"})
    @BeforeClass
    public void registerClient(final String userId, final String userSecret, String redirectUris) throws Exception {
        Reporter.log("Register client", true);

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<String> redirectUrisList = StringUtils.spaceSeparatedToList(redirectUris);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth benchmark test app", redirectUrisList);
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
    @Test(invocationCount = 1000, threadPoolSize = 10, dependsOnMethods = {"testAuthorization1"})
    public void testAuthorization2(final String userId, final String userSecret) throws Exception {
        testAuthorizationImpl(userId, userSecret, this.redirectUri, this.clientId);
    }

    @Parameters({"userId", "userSecret"})
    @Test(invocationCount = 500, threadPoolSize = 2, dependsOnMethods = {"testAuthorization2"})
    public void testAuthorization3(final String userId, final String userSecret) throws Exception {
        testAuthorizationImpl(userId, userSecret, this.redirectUri, this.clientId);
    }

    private void testAuthorizationImpl(final String userId, final String userSecret, String redirectUri, String clientId) {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "user_name");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);

        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        authorizationRequest.setState(state);
        authorizationRequest.setNonce(nonce);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(this.authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse response = authorizeClient.exec();

        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getCode(), "The authorization code is null");
        assertNotNull(response.getIdToken(), "The id_token is null");
        assertNotNull(response.getState(), "The state is null");
        assertNotNull(response.getScope(), "The scope is null");
    }

}

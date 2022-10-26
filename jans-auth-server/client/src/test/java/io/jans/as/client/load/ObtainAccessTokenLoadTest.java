/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.load;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.ClientUtils;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import io.jans.as.model.util.Util;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * DON'T INCLUDE IT IN TEST SUITE.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/12/2013
 */

public class ObtainAccessTokenLoadTest extends BaseTest {

    // Think twice before invoking this test ;). Leads to OpenDJ (Berkley DB) failure
    // Caused by: LDAPSearchException(resultCode=80 (other), numEntries=0, numReferences=0, errorMessage='Database exception: (JE 4.1.10) JAVA_ERROR: Java Error occurred, recovery may not be possible.')
    // http://ox.gluu.org/doku.php?id=oxauth:profiling#obtain_access_token_-_2000_invocations_within_200_concurrent_threads
    @Parameters({"userId", "userSecret", "redirectUris"})
    @Test(invocationCount = 1000, threadPoolSize = 100)
    public void obtainAccessToken(final String userId, final String userSecret, String redirectUris) throws Exception {
        showTitle("requestClientAssociate1");

        redirectUris = "https://client.example.com/cb";

        final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();

        final String clientId = response.getClientId();
        final String clientSecret = response.getClientSecret();

        // 1. Request authorization and receive the authorization code.

        final List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUris, null);
        request.setState("af0ifjsldkj");
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        final AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        final AuthorizationResponse response1 = authorizeClient.exec();

        ClientUtils.showClient(authorizeClient);

        final String scope = response1.getScope();
        final String authorizationCode = response1.getCode();
        assertTrue(Util.allNotBlank(authorizationCode));


        // 2. Request access token using the authorization code.
        final TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUris);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setScope(scope);

        final TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        tokenClient1.setRequest(tokenRequest);
        final TokenResponse response2 = tokenClient1.exec();
        ClientUtils.showClient(authorizeClient);

        assertTrue(response2.getStatus() == 200);
        final String patToken = response2.getAccessToken();
        final String patRefreshToken = response2.getRefreshToken();
        assertTrue(Util.allNotBlank(patToken, patRefreshToken));
    }

}

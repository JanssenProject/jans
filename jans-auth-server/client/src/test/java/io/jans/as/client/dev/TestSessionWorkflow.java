/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.dev;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import junit.framework.Assert;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;

/**
 * @version August 9, 2017
 */
public class TestSessionWorkflow extends BaseTest {

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void test(final String userId, final String userSecret,
                     final String clientId, final String clientSecret,
                     final String redirectUri) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            CookieStore cookieStore = new BasicCookieStore();
            httpClient.setCookieStore(cookieStore);
            ApacheHttpClient43Engine clientExecutor = new ApacheHttpClient43Engine(httpClient);

            ////////////////////////////////////////////////
            //             TV side. Code 1                //
            ////////////////////////////////////////////////

            AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(
                    Arrays.asList(ResponseType.CODE),
                    clientId,
                    Arrays.asList("openid", "profile", "email"),
                    redirectUri,
                    null);

            authorizationRequest1.setAuthUsername(userId);
            authorizationRequest1.setAuthPassword(userSecret);
            authorizationRequest1.getPrompts().add(Prompt.NONE);
            authorizationRequest1.setState("af0ifjsldkj");
            authorizationRequest1.setRequestSessionId(true);

            AuthorizeClient authorizeClient1 = new AuthorizeClient(authorizationEndpoint);
            authorizeClient1.setRequest(authorizationRequest1);
            AuthorizationResponse authorizationResponse1 = authorizeClient1.exec(clientExecutor);

            //        showClient(authorizeClient1, cookieStore);

            String code1 = authorizationResponse1.getCode();
            String sessionId = authorizationResponse1.getSessionId();
            Assert.assertNotNull("code1 is null", code1);
            Assert.assertNotNull("sessionId is null", sessionId);

            // TV sends the code to the Backend
            // We don't use httpClient and cookieStore during this call


            ////////////////////////////////////////////////
            //             Backend  1 side. Code 1        //
            ////////////////////////////////////////////////


            // Get the access token
            TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
            TokenResponse tokenResponse1 = tokenClient1.execAuthorizationCode(code1, redirectUri, clientId, clientSecret);

            String accessToken1 = tokenResponse1.getAccessToken();
            Assert.assertNotNull("accessToken1 is null", accessToken1);

            // Get the user's claims
            UserInfoClient userInfoClient1 = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse userInfoResponse1 = userInfoClient1.execUserInfo(accessToken1);
            assertEquals(userInfoResponse1.getStatus() , 200, "Unexpected response code: " + userInfoResponse1.getStatus());

            ////////////////////////////////////////////////
            //             TV side. Code 2                //
            ////////////////////////////////////////////////

            AuthorizationRequest authorizationRequest2 = new AuthorizationRequest(
                    Arrays.asList(ResponseType.CODE),
                    clientId,
                    Arrays.asList("openid", "profile", "email"),
                    redirectUri,
                    null);

            authorizationRequest2.getPrompts().add(Prompt.NONE);
            authorizationRequest2.setState("af0ifjsldkj");
            authorizationRequest2.setSessionId(sessionId);

            AuthorizeClient authorizeClient2 = new AuthorizeClient(authorizationEndpoint);
            authorizeClient2.setRequest(authorizationRequest2);
            AuthorizationResponse authorizationResponse2 = authorizeClient2.exec(clientExecutor);

            //        showClient(authorizeClient2, cookieStore);

            String code2 = authorizationResponse2.getCode();
            Assert.assertNotNull("code2 is null", code2);


            // TV sends the code to the Backend
            // We don't use httpClient and cookieStore during this call


            ////////////////////////////////////////////////
            //             Backend  2 side. Code 2        //
            ////////////////////////////////////////////////


            // Get the access token
            TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
            TokenResponse tokenResponse2 = tokenClient2.execAuthorizationCode(code2, redirectUri, clientId, clientSecret);

            String accessToken2 = tokenResponse2.getAccessToken();
            Assert.assertNotNull("accessToken2 is null", accessToken2);

            // Get the user's claims
            UserInfoClient userInfoClient2 = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse userInfoResponse2 = userInfoClient2.execUserInfo(accessToken2);

            assertEquals(userInfoResponse2.getStatus(), 200, "Unexpected response code: " + userInfoResponse2.getStatus());
        } finally {
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
        }
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    //@Test
    public void stressTest(final String userId, final String userSecret,
                           final String clientId, final String clientSecret,
                           final String redirectUri) throws Exception {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            System.out.println(i);
            test(userId, userSecret, clientId, clientSecret, redirectUri);
        }
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime) / 1000);
    }
}
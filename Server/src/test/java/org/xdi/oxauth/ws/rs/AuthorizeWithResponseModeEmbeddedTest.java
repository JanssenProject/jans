/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.model.authorize.AuthorizeResponseParam;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseMode;
import org.xdi.oxauth.model.common.ResponseType;

/**
 * @author Javier Rojas Blum Date: 03.24.2014
 */
public class AuthorizeWithResponseModeEmbeddedTest extends BaseTest {

    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestAuthorizationCodeWithResponseModeQuery(
            final String authorizePath, final String userId, final String userSecret, final String clientId,
            final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.CODE);
                List<String> scopes = new ArrayList<String>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("address");
                scopes.add("email");

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, null);
                authorizationRequest.setState("af0ifjsldkj");
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);
                authorizationRequest.setResponseMode(ResponseMode.QUERY);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestAuthorizationCodeWithResponseModeQuery", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
                    assertNotNull(uri.getQuery(), "Query string is null");

                    Map<String, String> params = QueryStringDecoder.decode(uri.getQuery());

                    assertNotNull(params.get(AuthorizeResponseParam.CODE), "The code is null");
                    assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
                    assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    fail("Response URI is not well formed");
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestAuthorizationCodeWithResponseModeFragment(
            final String authorizePath, final String userId, final String userSecret, final String clientId,
            final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.CODE);
                List<String> scopes = new ArrayList<String>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("address");
                scopes.add("email");

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, null);
                authorizationRequest.setState("af0ifjsldkj");
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);
                authorizationRequest.setResponseMode(ResponseMode.FRAGMENT);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestAuthorizationCodeWithResponseModeFragment", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
                    assertNotNull(uri.getFragment(), "Fragment is null");

                    Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                    assertNotNull(params.get(AuthorizeResponseParam.CODE), "The code is null");
                    assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
                    assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    fail("Response URI is not well formed");
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestAuthorizationTokenWithResponseModeQuery(
            final String authorizePath, final String userId, final String userSecret, final String clientId,
            final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.TOKEN);
                responseTypes.add(ResponseType.ID_TOKEN);
                List<String> scopes = new ArrayList<String>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("address");
                scopes.add("email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, nonce);
                authorizationRequest.setState("af0ifjsldkj");
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);
                authorizationRequest.setResponseMode(ResponseMode.QUERY);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestAuthorizationTokenWithResponseModeQuery", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
                        assertNotNull(uri.getQuery(), "Query is null");

                        Map<String, String> params = QueryStringDecoder.decode(uri.getQuery());

                        assertNotNull(params.get("access_token"), "The access token is null");
                        assertNotNull(params.get("state"), "The state is null");
                        assertNotNull(params.get("token_type"), "The token type is null");
                        assertNotNull(params.get("expires_in"), "The expires in value is null");
                        assertNotNull(params.get("scope"), "The scope must be null");
                        assertNull(params.get("refresh_token"), "The refresh_token must be null");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        fail("Response URI is not well formed");
                    }
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestAuthorizationTokenWithResponseModeFragment(
            final String authorizePath, final String userId, final String userSecret, final String clientId,
            final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.TOKEN);
                responseTypes.add(ResponseType.ID_TOKEN);
                List<String> scopes = new ArrayList<String>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("address");
                scopes.add("email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, nonce);
                authorizationRequest.setState("af0ifjsldkj");
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);
                authorizationRequest.setResponseMode(ResponseMode.FRAGMENT);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestAuthorizationTokenWithResponseModeFragment", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
                        assertNotNull(uri.getFragment(), "Fragment is null");

                        Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                        assertNotNull(params.get("access_token"), "The access token is null");
                        assertNotNull(params.get("state"), "The state is null");
                        assertNotNull(params.get("token_type"), "The token type is null");
                        assertNotNull(params.get("expires_in"), "The expires in value is null");
                        assertNotNull(params.get("scope"), "The scope must be null");
                        assertNull(params.get("refresh_token"), "The refresh_token must be null");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        fail("Response URI is not well formed");
                    }
                }
            }
        }.run();
    }
}
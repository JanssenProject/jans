/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.model.authorize.AuthorizeResponseParam;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;

/**
 * Functional tests for the Client Authentication Filter (embedded)
 *
 * @author Javier Rojas Blum
 * @version @version June 23, 2015
 */
public class ClientAuthenticationFilterEmbeddedTest extends BaseTest {

    private String customAttrValue1;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestClientRegistrationWithCustomAttributes(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.CODE,
                            ResponseType.TOKEN,
                            ResponseType.ID_TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));

                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    customAttrValue1 = UUID.randomUUID().toString();
                    registerRequest.addCustomAttribute("myCustomAttr1", customAttrValue1);

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestClientRegistrationWithCustomAttributes", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_ACCESS_TOKEN.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
    public void requestAccessTokenCustomClientAuth1(final String authorizePath,
                                                    final String userId, final String userSecret,
                                                    final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();
        final String nonce = UUID.randomUUID().toString();

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(
                        ResponseType.CODE,
                        ResponseType.ID_TOKEN);
                List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, customAttrValue1, scopes, redirectUri, nonce);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestAccessTokenCustomClientAuth1", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
                    assertNotNull(uri.getFragment(), "Query string is null");

                    Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                    assertNotNull(params.get(AuthorizeResponseParam.CODE), "The code is null");
                    assertNotNull(params.get(AuthorizeResponseParam.ID_TOKEN), "The id token is null");
                    assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                    assertEquals(params.get(AuthorizeResponseParam.STATE), state);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    fail("Response URI is not well formed");
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
    public void requestAccessTokenCustomClientAuth2(final String authorizePath,
                                                    final String userId, final String userSecret,
                                                    final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(
                        ResponseType.TOKEN,
                        ResponseType.ID_TOKEN);
                List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, customAttrValue1, scopes, redirectUri, nonce);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestAccessTokenCustomClientAuth2", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
                        assertNotNull(uri.getFragment(), "Fragment is null");

                        Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                        assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The access_token is null");
                        assertNotNull(params.get(AuthorizeResponseParam.ID_TOKEN), "The id_token is null");
                        assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                        assertNotNull(params.get(AuthorizeResponseParam.TOKEN_TYPE), "The token type is null");
                        assertNotNull(params.get(AuthorizeResponseParam.EXPIRES_IN), "The expires_in value is null");
                        assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope must be null");
                        assertNull(params.get("refresh_token"), "The refresh_token must be null");
                        assertEquals(params.get(AuthorizeResponseParam.STATE), state);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        fail("Response URI is not well formed");
                    }
                }
            }
        }.run();
    }

    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes", enabled = false)
    public void requestAccessTokenCustomClientAuth3(final String tokenPath,
                                                    final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("openid profile email");
                tokenRequest.setAuthUsername(customAttrValue1);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestAccessTokenCustomClientAuth3", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertTrue(!response.getContentAsString().equals(null), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
                    assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }
}
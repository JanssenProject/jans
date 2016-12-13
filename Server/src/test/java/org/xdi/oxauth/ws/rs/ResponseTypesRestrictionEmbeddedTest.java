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
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.model.authorize.AuthorizeResponseParam;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version December 12, 2016
 */
public class ResponseTypesRestrictionEmbeddedTest extends BaseTest {

    private String clientId1;
    private String clientSecret1;
    private String registrationAccessToken1;
    private String registrationClientUri1;
    private String authorizationCode1;

    private String clientId2;
    private String clientSecret2;
    private String registrationAccessToken2;
    private String registrationClientUri2;
    private String authorizationCode2;

    private String clientId3;
    private String registrationAccessToken3;
    private String registrationClientUri3;

    /**
     * Registering without provide the response_types param, should register the Client using only
     * the <code>code</code> response type.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void omittedResponseTypesStep1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

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
                showResponse("omittedResponseTypesStep1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    clientId1 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                    clientSecret1 = jsonObj.getString(CLIENT_SECRET.toString());
                    registrationAccessToken1 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
                    registrationClientUri1 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Client read request to verify the Client using the default <code>code</code> response type.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "omittedResponseTypesStep1")
    public void omittedResponseTypesStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken1);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedResponseTypesStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    // Registered Metadata
                    assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
                    assertNotNull(jsonObj.optJSONArray(RESPONSE_TYPES.toString()));
                    assertEquals(jsonObj.getJSONArray(RESPONSE_TYPES.toString()).getString(0),
                            ResponseType.CODE.toString());
                    assertTrue(jsonObj.has(REDIRECT_URIS.toString()));
                    assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
                    assertTrue(jsonObj.has(CLIENT_NAME.toString()));
                    assertTrue(jsonObj.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Request Authorization with Response Type <code>code</code> should succeed.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedResponseTypesStep2")
    public void omittedResponseTypesStep3a(final String authorizePath, final String userId, final String userSecret,
                                           final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(
                        ResponseType.CODE);
                List<String> scopes = Arrays.asList(
                        "openid",
                        "profile",
                        "address",
                        "email");
                String state = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId1, scopes, redirectUri, null);
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
                showResponse("omittedResponseTypesStep3a", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
                        assertNotNull(uri.getQuery(), "The query string is null");

                        Map<String, String> params = QueryStringDecoder.decode(uri.getQuery());

                        assertNotNull(params.get(AuthorizeResponseParam.CODE), "The code is null");
                        assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
                        assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                        assertFalse(params.containsKey(AuthorizeResponseParam.ID_TOKEN));
                        assertFalse(params.containsKey(AuthorizeResponseParam.ACCESS_TOKEN));

                        authorizationCode1 = params.get(AuthorizeResponseParam.CODE);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        fail("Response URI is not well formed");
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail(e.getMessage());
                    }
                }
            }
        }.run();
    }

    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"omittedResponseTypesStep3a"})
    public void omittedResponseTypesStep3b(final String tokenPath, final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setCode(authorizationCode1);
                tokenRequest.setRedirectUri(redirectUri);
                tokenRequest.setAuthUsername(clientId1);
                tokenRequest.setAuthPassword(clientSecret1);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedResponseTypesStep3b", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
                    assertTrue(jsonObj.has("id_token"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }

    @DataProvider(name = "omittedResponseTypesStep4DataProvider")
    public Object[][] omittedResponseTypesStep4DataProvider(ITestContext context) {
        String authorizePath = context.getCurrentXmlTest().getParameter("authorizePath");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");

        return new Object[][]{
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.ID_TOKEN)},
        };
    }

    /**
     * Authorization request with the other Response types combination should fail.
     */
    @Test(dependsOnMethods = "omittedResponseTypesStep3b", dataProvider = "omittedResponseTypesStep4DataProvider")
    public void omittedResponseTypesStep4(final String authorizePath, final String userId, final String userSecret,
                                          final String redirectUri, final List<ResponseType> responseTypes)
            throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<String> scopes = Arrays.asList(
                        "openid",
                        "profile",
                        "address",
                        "email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId1, scopes, redirectUri, nonce);
                authorizationRequest.setState("af0ifjsldkj");
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
                showResponse("omittedResponseTypesStep4", response);

                if (response.getStatus() == 400) {
                    assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                    try {
                        JSONObject jsonObj = new JSONObject(response.getContentAsString());
                        assertTrue(jsonObj.has("error"), "The error type is null");
                        assertTrue(jsonObj.has("error_description"), "The error description is null");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                    }
                } else {
                    fail("Unexpected response code: " + response.getStatus());
                }
            }
        }.run();
    }

    /**
     * Registering with the response_types param <code>code, id_token</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void responseTypesCodeIdTokenStep1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.CODE,
                            ResponseType.ID_TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

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
                showResponse("responseTypesCodeIdTokenStep1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    clientId2 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                    clientSecret2 = jsonObj.getString(CLIENT_SECRET.toString());
                    registrationAccessToken2 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
                    registrationClientUri2 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Client read request to verify the Client using the <code>code and id_token</code> response types.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "responseTypesCodeIdTokenStep1")
    public void responseTypesCodeIdTokenStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken2);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri2.substring(registrationClientUri2.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("responseTypesCodeIdTokenStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    // Registered Metadata
                    assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
                    assertNotNull(jsonObj.optJSONArray(RESPONSE_TYPES.toString()));
                    Set<String> responseTypes = new HashSet<String>();
                    for (int i = 0; i < jsonObj.getJSONArray(RESPONSE_TYPES.toString()).length(); i++) {
                        responseTypes.add(jsonObj.getJSONArray(RESPONSE_TYPES.toString()).getString(i));
                    }
                    assertTrue(responseTypes.containsAll(Arrays.asList(
                                    ResponseType.CODE.toString(),
                                    ResponseType.ID_TOKEN.toString()
                            )
                    ));
                    assertTrue(jsonObj.has(REDIRECT_URIS.toString()));
                    assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
                    assertTrue(jsonObj.has(CLIENT_NAME.toString()));
                    assertTrue(jsonObj.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Request Authorization with Response Type <code>code</code> should succeed.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "responseTypesCodeIdTokenStep2")
    public void responseTypesCodeIdTokenStep3a(final String authorizePath, final String userId, final String userSecret,
                                               final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(
                        ResponseType.CODE,
                        ResponseType.ID_TOKEN);
                List<String> scopes = Arrays.asList(
                        "openid",
                        "profile",
                        "address",
                        "email");
                String state = UUID.randomUUID().toString();
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId2, scopes, redirectUri, nonce);
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
                showResponse("responseTypesCodeIdTokenStep3a", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
                        assertNotNull(uri.getFragment(), "The fragment is null");

                        Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                        assertTrue(params.containsKey(AuthorizeResponseParam.CODE));
                        assertTrue(params.containsKey(AuthorizeResponseParam.SCOPE));
                        assertTrue(params.containsKey(AuthorizeResponseParam.STATE));
                        assertTrue(params.containsKey(AuthorizeResponseParam.ID_TOKEN));
                        assertFalse(params.containsKey(AuthorizeResponseParam.ACCESS_TOKEN));

                        authorizationCode2 = params.get(AuthorizeResponseParam.CODE);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        fail("Response URI is not well formed");
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail(e.getMessage());
                    }
                }
            }
        }.run();
    }

    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"responseTypesCodeIdTokenStep3a"})
    public void responseTypesCodeIdTokenStep3b(final String tokenPath, final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setCode(authorizationCode2);
                tokenRequest.setRedirectUri(redirectUri);
                tokenRequest.setAuthUsername(clientId2);
                tokenRequest.setAuthPassword(clientSecret2);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("responseTypesCodeIdTokenStep3b", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
                    assertTrue(jsonObj.has("id_token"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }

    @DataProvider(name = "responseTypesCodeIdTokenStep4DataProvider")
    public Object[][] responseTypesCodeIdTokenStep4DataProvider(ITestContext context) {
        String authorizePath = context.getCurrentXmlTest().getParameter("authorizePath");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");

        return new Object[][]{
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.ID_TOKEN)},
        };
    }

    /**
     * Authorization request with the other Response types combination should fail.
     */
    @Test(dependsOnMethods = "omittedResponseTypesStep3b", dataProvider = "responseTypesCodeIdTokenStep4DataProvider")
    public void responseTypesCodeIdTokenStep4(final String authorizePath, final String userId, final String userSecret,
                                              final String redirectUri, final List<ResponseType> responseTypes)
            throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<String> scopes = Arrays.asList(
                        "openid",
                        "profile",
                        "address",
                        "email"
                );
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId1, scopes, redirectUri, nonce);
                authorizationRequest.setState("af0ifjsldkj");
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
                showResponse("responseTypesCodeIdTokenStep4", response);

                if (response.getStatus() == 400) {
                    assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                    try {
                        JSONObject jsonObj = new JSONObject(response.getContentAsString());
                        assertTrue(jsonObj.has("error"), "The error type is null");
                        assertTrue(jsonObj.has("error_description"), "The error description is null");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                    }
                } else {
                    fail("Unexpected response code: " + response.getStatus());
                }
            }
        }.run();
    }

    /**
     * Registering with the response_types param <code>token, id_token</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void responseTypesTokenIdTokenStep1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.TOKEN,
                            ResponseType.ID_TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

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
                showResponse("responseTypesTokenIdTokenStep1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    clientId3 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                    registrationAccessToken3 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
                    registrationClientUri3 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Client read request to verify the Client using the <code>token and id_token</code> response types.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "responseTypesTokenIdTokenStep1")
    public void responseTypesTokenIdTokenStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken3);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri3.substring(registrationClientUri3.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("responseTypesTokenIdTokenStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    // Registered Metadata
                    assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
                    assertNotNull(jsonObj.optJSONArray(RESPONSE_TYPES.toString()));
                    Set<String> responseTypes = new HashSet<String>();
                    for (int i = 0; i < jsonObj.getJSONArray(RESPONSE_TYPES.toString()).length(); i++) {
                        responseTypes.add(jsonObj.getJSONArray(RESPONSE_TYPES.toString()).getString(i));
                    }
                    assertTrue(responseTypes.containsAll(Arrays.asList(
                                    ResponseType.TOKEN.toString(),
                                    ResponseType.ID_TOKEN.toString()
                            )
                    ));
                    assertTrue(jsonObj.has(REDIRECT_URIS.toString()));
                    assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
                    assertTrue(jsonObj.has(CLIENT_NAME.toString()));
                    assertTrue(jsonObj.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "responseTypesTokenIdTokenStep2")
    public void responseTypesTokenIdTokenStep3(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri)
            throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(
                        ResponseType.TOKEN,
                        ResponseType.ID_TOKEN);
                List<String> scopes = Arrays.asList(
                        "openid",
                        "profile",
                        "address",
                        "email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId3, scopes, redirectUri, nonce);
                authorizationRequest.setState("af0ifjsldkj");
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
                showResponse("responseTypesTokenIdTokenStep3", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
                        assertNotNull(uri.getFragment(), "Fragment is null");

                        Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                        assertNotNull(params.get("access_token"), "The access token is null");
                        assertNotNull(params.get("token_type"), "The token type is null");
                        assertNotNull(params.get("id_token"), "The id token is null");
                        assertNotNull(params.get("state"), "The state is null");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        fail("Response URI is not well formed");
                    }
                }
            }
        }.run();
    }

    @DataProvider(name = "responseTypesTokenIdTokenStep4DataProvider")
    public Object[][] responseTypesTokenIdTokenStep4DataProvider(ITestContext context) {
        String authorizePath = context.getCurrentXmlTest().getParameter("authorizePath");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");

        return new Object[][]{
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN)},
        };
    }

    /**
     * Authorization request with the other Response types combination should fail.
     */
    @Test(dependsOnMethods = "responseTypesTokenIdTokenStep3", dataProvider = "responseTypesTokenIdTokenStep4DataProvider")
    public void responseTypesTokenIdTokenStep4(final String authorizePath, final String userId, final String userSecret,
                                               final String redirectUri, final List<ResponseType> responseTypes)
            throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<String> scopes = Arrays.asList(
                        "openid",
                        "profile",
                        "address",
                        "email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId3, scopes, redirectUri, nonce);
                authorizationRequest.setState("af0ifjsldkj");
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
                showResponse("responseTypesTokenIdTokenStep4", response);

                if (response.getStatus() == 400) {
                    assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                    try {
                        JSONObject jsonObj = new JSONObject(response.getContentAsString());
                        assertTrue(jsonObj.has("error"), "The error type is null");
                        assertTrue(jsonObj.has("error_description"), "The error description is null");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                    }
                } else {
                    fail("Unexpected response code: " + response.getStatus());
                }
            }
        }.run();
    }
}
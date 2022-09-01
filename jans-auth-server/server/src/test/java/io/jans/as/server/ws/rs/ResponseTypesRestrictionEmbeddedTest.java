/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterResponseParam;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.jans.as.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static io.jans.as.model.register.RegisterRequestParam.CLIENT_NAME;
import static io.jans.as.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static io.jans.as.model.register.RegisterRequestParam.REDIRECT_URIS;
import static io.jans.as.model.register.RegisterRequestParam.RESPONSE_TYPES;
import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_ID_ISSUED_AT;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_SECRET;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT;
import static io.jans.as.model.register.RegisterResponseParam.REGISTRATION_CLIENT_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class ResponseTypesRestrictionEmbeddedTest extends BaseTest {

    private static String clientId1;
    private static String clientSecret1;
    private static String registrationAccessToken1;
    private static String registrationClientUri1;
    private static String authorizationCode1;
    private static String clientId2;
    private static String clientSecret2;
    private static String registrationAccessToken2;
    private static String registrationClientUri2;
    private static String authorizationCode2;
    private static String clientId3;
    private static String registrationAccessToken3;
    private static String registrationClientUri3;
    @ArquillianResource
    private URI url;

    /**
     * Registering without provide the response_types param, should register the
     * Client using only the <code>code</code> response type.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void omittedResponseTypesStep1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("omittedResponseTypesStep1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
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
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Client read request to verify the Client using the default
     * <code>code</code> response type.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "omittedResponseTypesStep1")
    public void omittedResponseTypesStep2(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken1);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedResponseTypesStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
            assertNotNull(jsonObj.optJSONArray(RESPONSE_TYPES.toString()));
            assertEquals(jsonObj.getJSONArray(RESPONSE_TYPES.toString()).getString(0), ResponseType.CODE.toString());
            assertTrue(jsonObj.has(REDIRECT_URIS.toString()));
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(CLIENT_NAME.toString()));
            assertTrue(jsonObj.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
            assertTrue(jsonObj.has(SCOPE.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Request Authorization with Response Type <code>code</code> should
     * succeed.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedResponseTypesStep2")
    public void omittedResponseTypesStep3a(final String authorizePath, final String userId, final String userSecret,
                                           final String redirectUri) throws Exception {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedResponseTypesStep3a", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
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

    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"omittedResponseTypesStep3a"})
    public void omittedResponseTypesStep3b(final String tokenPath, final String redirectUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode1);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId1);
        tokenRequest.setAuthPassword(clientSecret1);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("omittedResponseTypesStep3b", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString(Constants.PRAGMA) != null && response.getHeaderString(Constants.PRAGMA).equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString(Constants.PRAGMA));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
            assertTrue(jsonObj.has("id_token"));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @DataProvider(name = "omittedResponseTypesStep4DataProvider")
    public Object[][] omittedResponseTypesStep4DataProvider(ITestContext context) {
        String authorizePath = context.getCurrentXmlTest().getParameter("authorizePath");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");

        return new Object[][]{
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.ID_TOKEN)},};
    }

    /**
     * Authorization request with the other Response types combination should
     * fail.
     */
    @Test(dependsOnMethods = "omittedResponseTypesStep3b", dataProvider = "omittedResponseTypesStep4DataProvider")
    public void omittedResponseTypesStep4(final String authorizePath, final String userId, final String userSecret,
                                          final String redirectUri, final List<ResponseType> responseTypes) throws Exception {
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                redirectUri, nonce);
        authorizationRequest.setState("af0ifjsldkj");
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedResponseTypesStep4", response, entity);

        if (response.getStatus() == 400) {
            assertNotNull(entity, "Unexpected result: " + entity);
            try {
                JSONObject jsonObj = new JSONObject(entity);
                assertTrue(jsonObj.has("error"), "The error type is null");
                assertTrue(jsonObj.has("error_description"), "The error description is null");
            } catch (JSONException e) {
                e.printStackTrace();
                fail(e.getMessage() + "\nResponse was: " + entity);
            }
        } else {
            fail("Unexpected response code: " + response.getStatus());
        }
    }

    /**
     * Registering with the response_types param <code>code, id_token</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void responseTypesCodeIdTokenStep1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("responseTypesCodeIdTokenStep1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
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
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Client read request to verify the Client using the
     * <code>code and id_token</code> response types.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "responseTypesCodeIdTokenStep1")
    public void responseTypesCodeIdTokenStep2(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri2.substring(registrationClientUri2.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken2);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("responseTypesCodeIdTokenStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
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
            assertTrue(responseTypes
                    .containsAll(Arrays.asList(ResponseType.CODE.toString(), ResponseType.ID_TOKEN.toString())));
            assertTrue(jsonObj.has(REDIRECT_URIS.toString()));
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(CLIENT_NAME.toString()));
            assertTrue(jsonObj.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
            assertTrue(jsonObj.has(SCOPE.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Request Authorization with Response Type <code>code</code> should
     * succeed.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "responseTypesCodeIdTokenStep2")
    public void responseTypesCodeIdTokenStep3a(final String authorizePath, final String userId, final String userSecret,
                                               final String redirectUri) throws Exception {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId2, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("responseTypesCodeIdTokenStep3a", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
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

    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"responseTypesCodeIdTokenStep3a"})
    public void responseTypesCodeIdTokenStep3b(final String tokenPath, final String redirectUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode2);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId2);
        tokenRequest.setAuthPassword(clientSecret2);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("responseTypesCodeIdTokenStep3b", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString(Constants.PRAGMA) != null && response.getHeaderString(Constants.PRAGMA).equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString(Constants.PRAGMA));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
            assertTrue(jsonObj.has("id_token"));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @DataProvider(name = "responseTypesCodeIdTokenStep4DataProvider")
    public Object[][] responseTypesCodeIdTokenStep4DataProvider(ITestContext context) {
        String authorizePath = context.getCurrentXmlTest().getParameter("authorizePath");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");

        return new Object[][]{{authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.ID_TOKEN)},};
    }

    /**
     * Authorization request with the other Response types combination should
     * fail.
     */
    @Test(dependsOnMethods = "omittedResponseTypesStep3b", dataProvider = "responseTypesCodeIdTokenStep4DataProvider")
    public void responseTypesCodeIdTokenStep4(final String authorizePath, final String userId, final String userSecret,
                                              final String redirectUri, final List<ResponseType> responseTypes) throws Exception {
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                redirectUri, nonce);
        authorizationRequest.setState("af0ifjsldkj");
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("responseTypesCodeIdTokenStep4", response, entity);

        if (response.getStatus() == 400) {
            assertNotNull(entity, "Unexpected result: " + entity);
            try {
                JSONObject jsonObj = new JSONObject(entity);
                assertTrue(jsonObj.has("error"), "The error type is null");
                assertTrue(jsonObj.has("error_description"), "The error description is null");
            } catch (JSONException e) {
                e.printStackTrace();
                fail(e.getMessage() + "\nResponse was: " + entity);
            }
        } else {
            fail("Unexpected response code: " + response.getStatus());
        }
    }

    /**
     * Registering with the response_types param <code>token, id_token</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void responseTypesTokenIdTokenStep1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("responseTypesTokenIdTokenStep1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
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
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Client read request to verify the Client using the
     * <code>token and id_token</code> response types.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "responseTypesTokenIdTokenStep1")
    public void responseTypesTokenIdTokenStep2(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri3.substring(registrationClientUri3.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken3);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("responseTypesTokenIdTokenStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
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
            assertTrue(responseTypes
                    .containsAll(Arrays.asList(ResponseType.TOKEN.toString(), ResponseType.ID_TOKEN.toString())));
            assertTrue(jsonObj.has(REDIRECT_URIS.toString()));
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(CLIENT_NAME.toString()));
            assertTrue(jsonObj.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
            assertTrue(jsonObj.has(SCOPE.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "responseTypesTokenIdTokenStep2")
    public void responseTypesTokenIdTokenStep3(final String authorizePath, final String userId, final String userSecret,
                                               final String redirectUri) throws Exception {
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId3, scopes,
                redirectUri, nonce);
        authorizationRequest.setState("af0ifjsldkj");
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("responseTypesTokenIdTokenStep3", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
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

    @DataProvider(name = "responseTypesTokenIdTokenStep4DataProvider")
    public Object[][] responseTypesTokenIdTokenStep4DataProvider(ITestContext context) {
        String authorizePath = context.getCurrentXmlTest().getParameter("authorizePath");
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");

        return new Object[][]{{authorizePath, userId, userSecret, redirectUri, Arrays.asList(ResponseType.CODE)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN)},
                {authorizePath, userId, userSecret, redirectUri,
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN)},};
    }

    /**
     * Authorization request with the other Response types combination should
     * fail.
     */
    @Test(dependsOnMethods = "responseTypesTokenIdTokenStep3", dataProvider = "responseTypesTokenIdTokenStep4DataProvider")
    public void responseTypesTokenIdTokenStep4(final String authorizePath, final String userId, final String userSecret,
                                               final String redirectUri, final List<ResponseType> responseTypes) throws Exception {
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId3, scopes,
                redirectUri, nonce);
        authorizationRequest.setState("af0ifjsldkj");
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("responseTypesTokenIdTokenStep4", response, entity);

        if (response.getStatus() == 400) {
            assertNotNull(entity, "Unexpected result: " + entity);
            try {
                JSONObject jsonObj = new JSONObject(entity);
                assertTrue(jsonObj.has("error"), "The error type is null");
                assertTrue(jsonObj.has("error_description"), "The error description is null");
            } catch (JSONException e) {
                e.printStackTrace();
                fail(e.getMessage() + "\nResponse was: " + entity);
            }
        } else {
            fail("Unexpected response code: " + response.getStatus());
        }
    }

}

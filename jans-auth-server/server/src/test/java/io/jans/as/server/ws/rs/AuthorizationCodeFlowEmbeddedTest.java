/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.RegisterRequest;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.server.util.TestUtil;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Test cases for the authorization code flow (embedded)
 *
 * @author Javier Rojas Blum
 * @version May 14, 2019
 */
public class AuthorizationCodeFlowEmbeddedTest extends BaseTest {

    private static String clientId;
    private static String clientSecret;
    private static String authorizationCode1;
    private static String authorizationCode2;
    private static String authorizationCode3;
    private static String authorizationCode4;
    private static String accessToken1;
    private static String refreshToken1;
    private static String refreshToken2;
    private static String refreshToken3;
    @ArquillianResource
    private URI url;
    @Inject
    private AppConfiguration appConfiguration;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            io.jans.as.client.RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("dynamicClientRegistration", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            final io.jans.as.client.RegisterResponse registerResponse = io.jans.as.client.RegisterResponse.valueOf(entity);
            TestUtil.assert_(registerResponse);

            clientId = registerResponse.getClientId();
            clientSecret = registerResponse.getClientSecret();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Test for the complete Authorization Code Flow: 1. Request authorization
     * and receive the authorization code. 2. Request access token using the
     * authorization code. 3. Validate access token. 4. Request new access token
     * using the refresh token.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration", priority = 10)
    public void completeFlowStep1(final String authorizePath, final String userId, final String userSecret,
                                  final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
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
        showResponse("completeFlowStep1", response, entity);

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
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);

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

    @Parameters({"tokenPath", "validateTokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "completeFlowStep1"}, priority = 10)
    public void completeFlowStep2(final String tokenPath, final String validateTokenPath, final String redirectUri)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode1);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("completeFlowStep2", response, entity);

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
            assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");

            String accessToken = jsonObj.getString("access_token");
            refreshToken2 = jsonObj.getString("refresh_token");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"tokenPath"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "completeFlowStep2"}, priority = 10)
    public void completeFlowStep3(final String tokenPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.REFRESH_TOKEN);
        tokenRequest.setRefreshToken(refreshToken2);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("completeFlowStep3", response, entity);

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
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration", priority = 20)
    public void completeFlowWithOptionalNonceStep1(final String authorizePath, final String userId,
                                                   final String userSecret, final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
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

        showResponse("completeFlowWithOptionalNonceStep1", response, entity);

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
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);

                authorizationCode4 = params.get(AuthorizeResponseParam.CODE);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Parameters({"tokenPath", "validateTokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "completeFlowWithOptionalNonceStep1"}, priority = 20)
    public void completeFlowWithOptionalNonceStep2(final String tokenPath, final String validateTokenPath,
                                                   final String redirectUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode4);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("completeFlowWithOptionalNonceStep2", response, entity);

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
            assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");

            String accessToken = jsonObj.getString("access_token");
            refreshToken3 = jsonObj.getString("refresh_token");
            String idToken = jsonObj.getString("id_token");
            Jwt jwt = Jwt.parse(idToken);
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.NONCE));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"tokenPath"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "completeFlowWithOptionalNonceStep2"}, priority = 20)
    public void completeFlowWithOptionalNonceStep3(final String tokenPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.REFRESH_TOKEN);
        tokenRequest.setRefreshToken(refreshToken3);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("completeFlowWithOptionalNonceStep3", response, entity);

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
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * When an authorization code is used more than once, all the tokens issued
     * for that authorization code must be revoked: 1. Request authorization and
     * receive the authorization code. 2. Request access token using the
     * authorization code. 3. Request access token using the same authorization
     * code one more time. This call must fail. 4. Request new access token
     * using the refresh token. This call must fail too. 5. Request user info
     * must fail.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration", priority = 30)
    public void revokeTokensStep1(final String authorizePath, final String userId, final String userSecret,
                                  final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, null);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.setState(state);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();

        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("revokeTokensStep1", response, entity);

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
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);

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
    @Test(dependsOnMethods = {"dynamicClientRegistration", "revokeTokensStep1"}, priority = 30)
    public void revokeTokensStep2n3(final String tokenPath, final String redirectUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();
        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode2);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("revokeTokensStep2n3", response, entity);

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
            assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");

            accessToken1 = jsonObj.getString("access_token");
            refreshToken1 = jsonObj.getString("refresh_token");

            Builder request2 = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();
            io.jans.as.client.TokenRequest tokenRequest2 = new io.jans.as.client.TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest2.setCode(authorizationCode2);
            tokenRequest2.setRedirectUri(redirectUri);
            tokenRequest2.setAuthUsername(clientId);
            tokenRequest2.setAuthPassword(clientSecret);

            request2.header("Authorization", "Basic " + tokenRequest2.getEncodedCredentials());

            Response response2 = request2
                    .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest2.getParameters())));
            String entity2 = response2.readEntity(String.class);

            showResponse("revokeTokens step 3", response2, entity2);

            assertEquals(response2.getStatus(), 400, "Unexpected response code.");
            assertNotNull(entity2, "Unexpected result: " + entity2);
            try {
                JSONObject jsonObj2 = new JSONObject(entity2);
                assertTrue(jsonObj2.has("error"), "The error type is null");
                assertTrue(jsonObj2.has("error_description"), "The error description is null");
            } catch (JSONException e) {
                e.printStackTrace();
                fail(e.getMessage() + "\nResponse was: " + entity2);
            }
        } catch (

                JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"tokenPath"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "revokeTokensStep2n3"}, priority = 30)
    public void revokeTokensStep4(final String tokenPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.REFRESH_TOKEN);
        tokenRequest.setRefreshToken(refreshToken1);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("revokeTokensStep4", response, entity);

        assertEquals(response.getStatus(), 400, "Unexpected response code.");
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("error"), "The error type is null");
            assertTrue(jsonObj.has("error_description"), "The error description is null");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "revokeTokensStep4", priority = 30)
    public void revokeTokensStep5(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();

        request.header("Authorization", "Bearer " + accessToken1);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("revokeTokensStep5", response, entity);

        assertEquals(response.getStatus(), 401, "Unexpected response code.");
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("error"), "The error type is null");
            assertTrue(jsonObj.has("error_description"), "The error description is null");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Test to verify the token expiration 1. Request authorization and receive
     * the authorization code. ...Wait until the authorization code expires...
     * 2. Request access token using the expired authorization code. This call
     * must fail.
     *
     * @throws Exception
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration", priority = 40)
    public void tokenExpirationStep1(final String authorizePath, final String userId, final String userSecret,
                                     final String redirectUri) throws Exception {

        // Store current configuration
        int currentAuthorizationCodeLifetime = appConfiguration.getAuthorizationCodeLifetime();
        int currentCleanServiceInterval = appConfiguration.getCleanServiceInterval();

        // We need to expire in test test code token faster than usual to avoid sleeping test for long time
        appConfiguration.setAuthorizationCodeLifetime(8);
        appConfiguration.setCleanServiceInterval(6);
        try {
            final String state = UUID.randomUUID().toString();

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

            io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                    redirectUri, null);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);
            authorizationRequest.setState(state);

            Builder request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();

            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);

            Response response = request.get();
            String entity = response.readEntity(String.class);

            showResponse("tokenExpirationStep1", response, entity);

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
                    assertEquals(params.get(AuthorizeResponseParam.STATE), state);

                    authorizationCode3 = params.get(AuthorizeResponseParam.CODE);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    fail("Response URI is not well formed");
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        } finally {
            // Restore configuration
            appConfiguration.setAuthorizationCodeLifetime(currentAuthorizationCodeLifetime);
            appConfiguration.setCleanServiceInterval(currentCleanServiceInterval);
        }
    }

    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "tokenExpirationStep1"}, priority = 40)
    public void tokenExpirationStep2(final String tokenPath, final String redirectUri) throws Exception {
        // ...Wait until the authorization code expires...
        System.out.println("Sleeping for 20 seconds .....");
        Thread.sleep(20000);

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode3);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenExpirationStep2", response, entity);

        assertEquals(response.getStatus(), 400, "Unexpected response code.");
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("error"), "The error type is null");
            assertTrue(jsonObj.has("error_description"), "The error description is null");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }
}
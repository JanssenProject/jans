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
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.config.Constants;
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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Functional tests for Client Info Web Services (embedded)
 *
 * @author Javier Rojas Blum
 * @version March 9, 2019
 */
public class ClientInfoRestWebServiceEmbeddedTest extends BaseTest {

    private static String clientId;
    private static String clientSecret;
    private static String accessToken1;
    private static String accessToken2;
    private static String accessToken3;
    @ArquillianResource
    private URI url;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

            io.jans.as.client.RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            List<GrantType> grantTypes = Arrays.asList(
                    GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
            );
            registerRequest.setGrantTypes(grantTypes);

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

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestClientInfoStep1ImplicitFlow(final String authorizePath, final String userId,
                                                   final String userSecret, final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("clientinfo");
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

        showResponse("requestClientInfo step 1 Implicit Flow", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The access token is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertNotNull(params.get(AuthorizeResponseParam.TOKEN_TYPE), "The token type is null");
                assertNotNull(params.get(AuthorizeResponseParam.EXPIRES_IN), "The expires in value is null");
                assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope must be null");
                assertNull(params.get("refresh_token"), "The refresh_token must be null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);

                accessToken1 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unexpected error");
            }
        }
    }

    @Parameters({"clientInfoPath"})
    @Test(dependsOnMethods = "requestClientInfoStep1ImplicitFlow")
    public void requestClientInfoStep2PostImplicitFlow(final String clientInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + clientInfoPath).request();

        request.header("Authorization", "Bearer " + accessToken1);
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.ClientInfoRequest clientInfoRequest = new io.jans.as.client.ClientInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(clientInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestClientInfo step 2 POST Implicit Flow", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString(Constants.PRAGMA) != null && response.getHeaderString(Constants.PRAGMA).equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString(Constants.PRAGMA));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("name"), "Unexpected result: name not found");
            assertTrue(jsonObj.has("inum"), "Unexpected result: inum not found");
            assertTrue(jsonObj.has("jansAppType"), "Unexpected result: jansAppTyp not found");
            assertTrue(jsonObj.has("jansIdTknSignedRespAlg"),
                    "Unexpected result: jansIdTknSignedRespAlg not found");
            assertTrue(jsonObj.has("jansRedirectURI"), "Unexpected result: jansRedirectURI not found");
            assertTrue(jsonObj.has("jansScope"), "Unexpected result: jansScope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"clientInfoPath"})
    @Test(dependsOnMethods = "requestClientInfoStep1ImplicitFlow")
    public void requestClientInfoStep2GetImplicitFlow(final String clientInfoPath) throws Exception {

        io.jans.as.client.ClientInfoRequest clientInfoRequest = new io.jans.as.client.ClientInfoRequest(null);
        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + clientInfoPath + "?" + clientInfoRequest.getQueryString()).request();

        request.header("Authorization", "Bearer " + accessToken1);
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestClientInfo step 2 GET Implicit Flow", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString(Constants.PRAGMA) != null && response.getHeaderString(Constants.PRAGMA).equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString(Constants.PRAGMA));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("name"), "Unexpected result: name not found");
            assertTrue(jsonObj.has("inum"), "Unexpected result: inum not found");
            assertTrue(jsonObj.has("jansAppType"), "Unexpected result: jansAppTyp not found");
            assertTrue(jsonObj.has("jansIdTknSignedRespAlg"),
                    "Unexpected result: jansIdTknSignedRespAlg not found");
            assertTrue(jsonObj.has("jansRedirectURI"), "Unexpected result: jansRedirectURI not found");
            assertTrue(jsonObj.has("jansScope"), "Unexpected result: jansScope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestClientInfoStep1PasswordFlow(final String tokenPath, final String userId, final String userSecret)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("clientinfo");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestClientInfoStep1PasswordFlow", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString(Constants.PRAGMA) != null && response.getHeaderString(Constants.PRAGMA).equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString(Constants.PRAGMA));
        assertTrue(!entity.equals(null), "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");

            accessToken3 = jsonObj.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"clientInfoPath"})
    @Test(dependsOnMethods = "requestClientInfoStep1PasswordFlow")
    public void requestClientInfoStep2PasswordFlow(final String clientInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + clientInfoPath).request();

        request.header("Authorization", "Bearer " + accessToken3);
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.ClientInfoRequest clientInfoRequest = new io.jans.as.client.ClientInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(clientInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoStep2PasswordFlow", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString(Constants.PRAGMA) != null && response.getHeaderString(Constants.PRAGMA).equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("name"), "Unexpected result: name not found");
            assertTrue(jsonObj.has("inum"), "Unexpected result: inum not found");
            assertTrue(jsonObj.has("jansAppType"), "Unexpected result: oxAuthAppType not found");
            assertTrue(jsonObj.has("jansIdTknSignedRespAlg"),
                    "Unexpected result: oxAuthIdTokenSignedResponseAlg not found");
            assertTrue(jsonObj.has("jansRedirectURI"), "Unexpected result: oxAuthRedirectURI not found");
            assertTrue(jsonObj.has("jansScope"), "Unexpected result: oxAuthScope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"clientInfoPath"})
    @Test
    public void requestClientInfoInvalidRequest(final String clientInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + clientInfoPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.ClientInfoRequest clientInfoRequest = new io.jans.as.client.ClientInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(clientInfoRequest.getParameters())));

        String entity = response.readEntity(String.class);

        showResponse("requestClientInfoInvalidRequest", response, entity);

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

    @Parameters({"clientInfoPath"})
    @Test
    public void requestClientInfoInvalidToken(final String clientInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + clientInfoPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.ClientInfoRequest clientInfoRequest = new io.jans.as.client.ClientInfoRequest("INVALID-TOKEN");
        clientInfoRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(clientInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestClientInfoInvalidToken", response, entity);

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
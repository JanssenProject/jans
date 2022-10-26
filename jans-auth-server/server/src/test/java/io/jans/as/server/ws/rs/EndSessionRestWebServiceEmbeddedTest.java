/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.*;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import io.jans.as.server.util.TestUtil;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.jans.as.model.register.RegisterResponseParam.CLIENT_ID;
import static org.testng.Assert.*;

/**
 * Test cases for the end session web service (embedded)
 *
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class EndSessionRestWebServiceEmbeddedTest extends BaseTest {

    private static String clientId;
    private static String idToken;
    private static String sessionId;
    private static String sid;
    @ArquillianResource
    private URI url;

    @Parameters({"registerPath", "redirectUris", "postLogoutRedirectUri"})
    @Test
    public void requestEndSessionStep1(final String registerPath, final String redirectUris,
                                       final String postLogoutRedirectUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));
            registerRequest.setFrontChannelLogoutUri(postLogoutRedirectUri);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestEndSessionStep1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
            TestUtil.assert_(registerResponse);

            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(CLIENT_ID.toString()));

            clientId = jsonObj.getString(CLIENT_ID.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestEndSessionStep1")
    public void requestEndSessionStep2(final String authorizePath, final String userId, final String userSecret,
                                       final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
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

        showResponse("requestEndSessionStep2", response, entity);

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

                idToken = params.get(AuthorizeResponseParam.ID_TOKEN);
                sessionId = params.get(AuthorizeResponseParam.SESSION_ID);
                sid = params.get(AuthorizeResponseParam.SID);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Parameters({"endSessionPath", "postLogoutRedirectUri"})
    @Test(dependsOnMethods = "requestEndSessionStep2")
    public void requestEndSessionStep3(final String endSessionPath, final String postLogoutRedirectUri)
            throws Exception {
        String state = UUID.randomUUID().toString();

        EndSessionRequest endSessionRequest = new EndSessionRequest(idToken, postLogoutRedirectUri, state);
        endSessionRequest.setSid(sid);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + endSessionPath + "?" + endSessionRequest.getQueryString()).request();
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        request.cookie(AuthorizeResponseParam.SESSION_ID, sessionId);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestEndSessionStep3", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertNotNull(entity, "Unexpected html.");
        assertTrue(entity.contains(postLogoutRedirectUri));
        assertTrue(entity.contains(postLogoutRedirectUri));

    }

    // private void validateNonHttpBasedLogout(EnhancedMockHttpServletResponse
    // response) {
    // if (response.getLocation() != null) {
    // try {
    // URI uri = new URI(response.getLocation().toString());
    // assertNotNull(uri.getQuery(), "The query string is null");
    //
    // Map<String, String> params = QueryStringDecoder.decode(uri.getQuery());
    //
    // assertNotNull(params.get(EndSessionResponseParam.STATE), "The state is
    // null");
    // assertEquals(params.get(EndSessionResponseParam.STATE), endSessionId);
    // } catch (URISyntaxException e) {
    // e.printStackTrace();
    // fail("Response URI is not well formed");
    // } catch (Exception e) {
    // e.printStackTrace();
    // fail(e.getMessage());
    // }
    // }
    // }

    @Parameters({"endSessionPath"})
    @Test(enabled = true)
    // switched off test : WebApplicationException seems to not translated correctly into response by container and results in 500 error. See EndSessionRestWebServiceImpl.endSession()
    public void requestEndSessionFail1(final String endSessionPath) throws Exception {
        EndSessionRequest endSessionRequest = new EndSessionRequest(null, null, null);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + endSessionPath + "?" + endSessionRequest.getQueryString()).request();
        request.header("Content-Type", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestEndSessionFail1", response, entity);

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

    @Parameters({"endSessionPath", "postLogoutRedirectUri"})
    @Test
    public void requestEndSessionFail2(final String endSessionPath, final String postLogoutRedirectUri) {
        String endSessionId = UUID.randomUUID().toString();
        EndSessionRequest endSessionRequest = new EndSessionRequest("INVALID_ACCESS_TOKEN", postLogoutRedirectUri,
                endSessionId);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + endSessionPath + "?" + endSessionRequest.getQueryString()).request();
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestEndSessionFail2", response, entity);

        // we can get bad request or redirect to RP according to https://github.com/GluuFederation/oxAuth/issues/575
        assertTrue(response.getStatus() == 400 || response.getStatus() == 307, "Unexpected response code.");
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
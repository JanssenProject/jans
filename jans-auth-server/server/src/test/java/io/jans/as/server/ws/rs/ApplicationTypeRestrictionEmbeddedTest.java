/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.RegisterRequest;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterResponseParam;
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
import jakarta.ws.rs.core.Response;
import java.net.URI;

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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class ApplicationTypeRestrictionEmbeddedTest extends BaseTest {

    private static String registrationAccessToken1;
    private static String registrationClientUri1;
    private static String registrationAccessToken2;
    private static String registrationClientUri2;
    private static String registrationAccessToken3;
    private static String registrationClientUri3;

    @ArquillianResource
    private URI url;

    /**
     * Register a client without specify an Application Type.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void omittedApplicationTypeStep1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {

            RegisterRequest registerRequest = new RegisterRequest(null, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("omittedApplicationTypeStep1", response, entity);

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

            registrationAccessToken1 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri1 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the default Application Type
     * <code>web</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "omittedApplicationTypeStep1")
    public void omittedApplicationTypeStep2(final String registerPath) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken1);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedApplicationTypeStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertEquals(jsonObj.getString(APPLICATION_TYPE.toString()), ApplicationType.WEB.toString());
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
     * Register a client with Application Type <code>web</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void applicationTypeWebStep1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("applicationTypeWebStep1", response, entity);

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

            registrationAccessToken2 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri2 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Application Type
     * <code>web</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "applicationTypeWebStep1")
    public void applicationTypeWebStep2(final String registerPath) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri2.substring(registrationClientUri2.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken2);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("applicationTypeWebStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertEquals(jsonObj.getString(APPLICATION_TYPE.toString()), ApplicationType.WEB.toString());
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
     * Fail: Register a client with Application Type <code>web</code> and
     * Redirect URI with the schema HTTP.
     */
    @Parameters({"registerPath"})
    @Test
    public void applicationTypeWebFail1(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            final String redirectUris = "http://client.example.com/cb";

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("applicationTypeWebFail1", response, entity);

        assertEquals(response.getStatus(), 400, "Unexpected response code. " + entity);
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
     * Register a client with Application Type <code>native</code>.
     */
    @Parameters({"registerPath"})
    @Test
    public void applicationTypeNativeStep1(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            final String redirectUris = "http://localhost/cb";

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.NATIVE, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("applicationTypeNativeStep1", response, entity);

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

            registrationAccessToken3 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri3 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Application Type
     * <code>native</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "applicationTypeNativeStep1")
    public void applicationTypeNativeStep2(final String registerPath) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri3.substring(registrationClientUri3.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken3);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("applicationTypeNativeStep2", response);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertEquals(jsonObj.getString(APPLICATION_TYPE.toString()), ApplicationType.NATIVE.toString());
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
     * Fail: Register a client with Application Type <code>native</code> and
     * Redirect URI with the schema HTTPS.
     */
    @Parameters({"registerPath"})
    @Test(enabled = false) // allowed to register redirect_uris with custom
    // schema to conform "OAuth 2.0 for Native Apps"
    // spec
    public void applicationTypeNativeFail1(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            final String redirectUris = "https://client.example.com/cb";

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.NATIVE, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("applicationTypeNativeFail1", response, entity);

        assertEquals(response.getStatus(), 400, "Unexpected response code. " + entity);
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
     * Fail: Register a client with Application Type <code>native</code> and
     * Redirect URI with the host different than localhost.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test(enabled = false) // allowed to register redirect_uris with custom
    // schema to conform "OAuth 2.0 for Native Apps"
    // spec
    public void applicationTypeNativeFail2(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.NATIVE, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("applicationTypeNativeFail2", response, entity);

        assertEquals(response.getStatus(), 400, "Unexpected response code. " + entity);
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
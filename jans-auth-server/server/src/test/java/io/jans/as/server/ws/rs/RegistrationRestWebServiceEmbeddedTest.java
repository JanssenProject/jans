/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterResponseParam;
import io.jans.as.test.TestUtil;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.jans.as.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static io.jans.as.model.register.RegisterRequestParam.CLAIMS_REDIRECT_URIS;
import static io.jans.as.model.register.RegisterRequestParam.CLIENT_NAME;
import static io.jans.as.model.register.RegisterRequestParam.CLIENT_URI;
import static io.jans.as.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static io.jans.as.model.register.RegisterRequestParam.REDIRECT_URIS;
import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_ID_ISSUED_AT;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_SECRET;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT;
import static io.jans.as.server.util.TestUtil.assert_;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Functional tests for Client Registration Web Services (embedded)
 *
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class RegistrationRestWebServiceEmbeddedTest extends BaseTest {

    private static String registrationAccessToken1;
    private static String registrationClientUri1;
    @ArquillianResource
    private URI url;

    public static void readResponseAssert(Response response, String entity) {
        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(REDIRECT_URIS.toString()));
            assertTrue(jsonObj.has(CLAIMS_REDIRECT_URIS.toString()));
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(CLIENT_NAME.toString()));
            assertTrue(jsonObj.has(ID_TOKEN_SIGNED_RESPONSE_ALG.toString()));
            assertTrue(jsonObj.has(SCOPE.toString()));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestClientAssociate1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));

            registerRequest.setClaimsRedirectUris(StringUtils.spaceSeparatedToList(redirectUris));

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestClientAssociate1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
            assert_(registerResponse);

            registrationAccessToken1 = registerResponse.getRegistrationAccessToken();
            registrationClientUri1 = registerResponse.getRegistrationClientUri();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath", "redirectUris", "sectorIdentifierUri", "contactEmail1", "contactEmail2"})
    @Test
    public void requestClientAssociate2(final String registerPath, final String redirectUris,
                                        final String sectorIdentifierUri, final String contactEmail1, final String contactEmail2) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<String> contacts = Arrays.asList(contactEmail1, contactEmail2);

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setContacts(contacts);
            registerRequest.setScope(Arrays.asList("openid", "clientinfo", "profile", "email", "invalid_scope"));
            registerRequest.setLogoUri("http://www.gluu.org/wp-content/themes/gluursn/images/logo.png");
            registerRequest.setClientUri("http://www.gluu.org/company/team");
            registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
            registerRequest.setPolicyUri("http://www.gluu.org/policy");
            registerRequest.setJwksUri("http://www.gluu.org/jwks");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setSubjectType(SubjectType.PAIRWISE);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestClientAssociate2", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
            assert_(registerResponse);

            JSONObject jsonObj = new JSONObject(entity);

            // Registered Metadata
            assertTrue(jsonObj.has(CLIENT_URI.toString()));
            assertTrue(jsonObj.has(SCOPE.toString()));

            JSONArray scopesJsonArray = new JSONArray(StringUtils.spaceSeparatedToList(jsonObj.getString((SCOPE.toString()))));
            List<String> scopes = new ArrayList<>();
            for (int i = 0; i < scopesJsonArray.length(); i++) {
                scopes.add(scopesJsonArray.get(i).toString());
            }
            assertTrue(scopes.contains("openid"));
            assertTrue(scopes.contains("email"));
            assertTrue(scopes.contains("profile"));
            assertTrue(scopes.contains("clientinfo"));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "requestClientAssociate1")
    public void requestClientRead(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken1);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestClientRead", response, entity);
        readResponseAssert(response, entity);
    }

    @Parameters({"registerPath", "redirectUris", "contactEmail1", "contactEmail2"})
    @Test(dependsOnMethods = "requestClientAssociate1")
    public void requestClientUpdate(final String registerPath, final String redirectUris, final String contactEmail1,
                                    final String contactEmail2) throws Exception {
        final String contactEmailNewValue = contactEmail2;
        final String logoUriNewValue = "http://www.gluu.org/test/yuriy/logo.png";
        final String clientUriNewValue = "http://www.gluu.org/company/yuriy";

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1)).request();

        String registerRequestContent = null;
        try {
            final RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));

            registerRequest.setContacts(Arrays.asList(contactEmail1, contactEmailNewValue));
            registerRequest.setLogoUri(logoUriNewValue);
            registerRequest.setClientUri(clientUriNewValue);

            request.header("Authorization", "Bearer " + registrationAccessToken1);
            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        Response response = request.put(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestClientRead", response, entity);

        readResponseAssert(response, entity);

        try {
            // check whether values are really updated
            RegisterRequest r = RegisterRequest.fromJson(entity);
            assertTrue(r.getContacts() != null && r.getContacts().contains(contactEmailNewValue));
            assertEquals(clientUriNewValue, r.getClientUri());
            assertEquals(logoUriNewValue, r.getLogoUri());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath"})
    @Test
    public void failRegistration_whenRedirectUriIsNotSetForResponseTypeCode(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(null, null, null);
            registerRequest.setResponseTypes(Lists.newArrayList(ResponseType.CODE));

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestClientRegistrationFail 1", response, entity);

        assertEquals(response.getStatus(), 400, "Unexpected response code. " + entity);
        TestUtil.assertErrorResponse(entity);
    }

    @Parameters({"registerPath"})
    @Test
    public void requestClientRegistrationFail3(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    Arrays.asList("https://client.example.com/cb#fail_fragment"));

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestClientRegistrationFail3", response, entity);

        assertEquals(response.getStatus(), 400, "Unexpected response code. " + entity);
        TestUtil.assertErrorResponse(entity);
    }

}
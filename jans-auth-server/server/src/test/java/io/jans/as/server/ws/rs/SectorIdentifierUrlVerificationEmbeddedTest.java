/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
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
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.jans.as.model.register.RegisterResponseParam.CLIENT_ID_ISSUED_AT;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_SECRET;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT;
import static io.jans.as.model.register.RegisterResponseParam.REGISTRATION_ACCESS_TOKEN;
import static io.jans.as.model.register.RegisterResponseParam.REGISTRATION_CLIENT_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Functional tests for Sector Identifier URL Verification (embedded)
 *
 * @author Javier Rojas Blum
 * @version August 21, 2015
 */
public class SectorIdentifierUrlVerificationEmbeddedTest extends BaseTest {

    private static String clientId1;
    private static String clientSecret1;
    @ArquillianResource
    private URI url;

    @Parameters({"registerPath", "redirectUris", "sectorIdentifierUri"})
    @Test // This test requires a place to publish a sector identifier JSON
    // array of redirect URIs via HTTPS
    public void requestAuthorizationCodeWithSectorIdentifierStep1(final String registerPath, final String redirectUris,
                                                                  final String sectorIdentifierUri) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setSubjectType(SubjectType.PAIRWISE);

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAuthorizationCodeWithSectorIdentifierStep1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(REGISTRATION_ACCESS_TOKEN.toString()));
            assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            clientId1 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret1 = jsonObj.getString(CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    // This test requires a place to publish a sector identifier JSON array of
    // redirect URIs via HTTPS
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestAuthorizationCodeWithSectorIdentifierStep1")
    public void requestAuthorizationCodeWithSectorIdentifierStep2(final String authorizePath, final String userId,
                                                                  final String userSecret, final String redirectUri) throws Exception {

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
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

        showResponse("requestAuthorizationCodeWithSectorIdentifierStep2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment());

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get(AuthorizeResponseParam.CODE), "The code is null");
            assertNotNull(params.get(AuthorizeResponseParam.ID_TOKEN), "The ID Token is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");

            String idToken = params.get(AuthorizeResponseParam.ID_TOKEN);

            Jwt jwt = Jwt.parse(idToken);
            assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        } catch (InvalidJwtException e) {
            e.printStackTrace();
            fail("Invalid JWT");
        }
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void sectorIdentifierUrlVerificationFail1(final String registerPath, final String redirectUris)
            throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri("https://INVALID_SECTOR_IDENTIFIER_URL");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("sectorIdentifierUrlVerificationFail1", response, entity);

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

    @Parameters({"registerPath", "sectorIdentifierUri"})
    @Test // This test requires a place to publish a sector identifier JSON
    // array of redirect URIs via HTTPS
    public void sectorIdentifierUrlVerificationFail2(final String registerPath, final String sectorIdentifierUri)
            throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            String redirectUris = "https://INVALID_REDIRECT_URI https://client.example.com/cb https://client.example.com/cb1 https://client.example.com/cb2";

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("sectorIdentifierUrlVerificationFail2", response, entity);

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

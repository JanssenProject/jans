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
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.crypto.AuthCryptoProvider;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
 * Functional tests for the Use Authentication Filter (embedded)
 *
 * @author Javier Rojas Blum
 * @version April 26, 2017
 */
public class UserAuthenticationFilterEmbeddedTest extends BaseTest {

    private static String clientId;
    private static String clientSecret;
    private static String clientId1;
    private static String clientSecret1;
    private static String clientId2;
    private static String clientSecret2;
    private static String clientId3;
    private static String clientSecret3;
    private static String clientId4;
    private static String clientSecret4;
    private static String authorizationCode1;
    @ArquillianResource
    private URI url;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

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

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenCustomAuth1Step1(final String registerPath, final String redirectUris,
                                                   final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth1Step1", response, entity);

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

    @Parameters({"tokenPath", "userInum", "userEmail"})
    @Test(dependsOnMethods = "requestAccessTokenCustomAuth1Step1")
    public void requestAccessTokenCustomAuth1Step2(final String tokenPath, final String userInum,
                                                   final String userEmail) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId1);
        tokenRequest.setAuthPassword(clientSecret1);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.addCustomParameter("mail", userEmail);
        tokenRequest.addCustomParameter("inum", userInum);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth1Step2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString(Constants.PRAGMA) != null && response.getHeaderString(Constants.PRAGMA).equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertTrue(!entity.equals(null), "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenCustomAuth2Step1(final String registerPath, final String redirectUris,
                                                   final String jwksUri) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth2Step1", response, entity);

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

            clientId2 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret2 = jsonObj.getString(CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "requestAccessTokenCustomAuth2Step1")
    public void requestAccessTokenCustomAuth2Step2(final String tokenPath, final String userId, final String userSecret)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId2);
        tokenRequest.setAuthPassword(clientSecret2);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.addCustomParameter("uid", userId);
        tokenRequest.addCustomParameter("pwd", userSecret);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth2Step2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertTrue(!entity.equals(null), "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenCustomAuth3Step1(final String registerPath, final String redirectUris,
                                                   final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth3Step1", response, entity);

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

            clientId3 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret3 = jsonObj.getString(CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"tokenPath", "userInum", "userEmail"})
    @Test(dependsOnMethods = "requestAccessTokenCustomAuth3Step1")
    public void requestAccessTokenCustomAuth3Step2(final String tokenPath, final String userInum,
                                                   final String userEmail) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId3);
        tokenRequest.setAuthPassword(clientSecret3);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.addCustomParameter("mail", userEmail);
        tokenRequest.addCustomParameter("inum", userInum);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth3Step2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertTrue(!entity.equals(null), "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenCustomAuth4Step1(final String registerPath, final String redirectUris,
                                                   final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        io.jans.as.client.RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth4Step1", response, entity);

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

            clientId4 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret4 = jsonObj.getString(CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"tokenPath", "userId", "userSecret", "audience", "RS256_keyId", "dnName", "keyStoreFile",
            "keyStoreSecret"})
    @Test(dependsOnMethods = "requestAccessTokenCustomAuth4Step1")
    public void requestAccessTokenCustomAuth4Step2(final String tokenPath, final String userId, final String userSecret,
                                                   final String audience, final String keyId, final String dnName, final String keyStoreFile,
                                                   final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId4);
        tokenRequest.setAuthPassword(clientSecret4);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(audience);
        tokenRequest.addCustomParameter("uid", userId);
        tokenRequest.addCustomParameter("pwd", userSecret);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth4Step2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertTrue(!entity.equals(null), "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestAccessTokenCustomAuthStep1(final String authorizePath, final String userId,
                                                  final String userSecret, final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + authorizePath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.addCustomParameter("uid", userId);
        authorizationRequest.addCustomParameter("pwd", userSecret);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(authorizationRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuthStep1", response, entity);

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
    @Test(dependsOnMethods = {"requestAccessTokenCustomAuthStep1", "dynamicClientRegistration"})
    public void requestAccessTokenCustomAuthStep2(final String tokenPath, final String validateTokenPath,
                                                  final String redirectUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode1);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuthStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
            assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");

            String accessToken = jsonObj.getString("access_token");
            String refreshToken = jsonObj.getString("refresh_token");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}

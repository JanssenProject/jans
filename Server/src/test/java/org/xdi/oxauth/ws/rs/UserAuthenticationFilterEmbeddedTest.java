/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.authorize.AuthorizeResponseParam;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.register.RegisterResponseParam;
import org.gluu.oxauth.model.util.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.gluu.oxauth.model.register.RegisterResponseParam.*;
import static org.testng.Assert.*;

/**
 * Functional tests for the Use Authentication Filter (embedded)
 *
 * @author Javier Rojas Blum
 * @version April 26, 2017
 */
public class UserAuthenticationFilterEmbeddedTest extends BaseTest {

    @ArquillianResource
    private URI url;

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

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("dynamicClientRegistration", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
            ClientTestUtil.assert_(registerResponse);

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
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth1Step1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
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
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
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
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
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

        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth2Step1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
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
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
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
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
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
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth3Step1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
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
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
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
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
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
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenCustomAuth4Step1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
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
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
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
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
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

        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + authorizePath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
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
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
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
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
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

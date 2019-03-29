/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.gluu.oxauth.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static org.gluu.oxauth.model.register.RegisterRequestParam.CLIENT_NAME;
import static org.gluu.oxauth.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static org.gluu.oxauth.model.register.RegisterRequestParam.REDIRECT_URIS;
import static org.gluu.oxauth.model.register.RegisterRequestParam.RESPONSE_TYPES;
import static org.gluu.oxauth.model.register.RegisterRequestParam.SCOPE;
import static org.gluu.oxauth.model.register.RegisterRequestParam.TOKEN_ENDPOINT_AUTH_METHOD;
import static org.gluu.oxauth.model.register.RegisterResponseParam.CLIENT_ID_ISSUED_AT;
import static org.gluu.oxauth.model.register.RegisterResponseParam.CLIENT_SECRET;
import static org.gluu.oxauth.model.register.RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT;
import static org.gluu.oxauth.model.register.RegisterResponseParam.REGISTRATION_CLIENT_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.model.authorize.AuthorizeResponseParam;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.Prompt;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.register.RegisterResponseParam;
import org.gluu.oxauth.model.util.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.TokenRequest;

/**
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class TokenEndpointAuthMethodRestrictionEmbeddedTest extends BaseTest {

    @ArquillianResource
    private URI url;

    private static String clientId1;
    private static String registrationAccessToken1;
    private static String registrationClientUri1;

    private static String clientId2;
    private static String clientSecret2;
    private static String registrationAccessToken2;
    private static String authorizationCode2;
    private static String registrationClientUri2;

    private static String clientId3;
    private static String clientSecret3;
    private static String registrationAccessToken3;
    private static String authorizationCode3;
    private static String registrationClientUri3;

    private static String clientId4;
    private static String clientSecret4;
    private static String registrationAccessToken4;
    private static String authorizationCode4;
    private static String registrationClientUri4;

    private static String clientId5;
    private static String clientSecret5;
    private static String registrationAccessToken5;
    private static String authorizationCode5;
    private static String registrationClientUri5;

    /**
     * Register a client without specify a Token Endpoint Auth Method.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void omittedTokenEndpointAuthMethodStep1(final String registerPath, final String redirectUris)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));

            registerRequestContent = registerRequest.getJSONParameters().toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("omittedTokenEndpointAuthMethodStep1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
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
            registrationAccessToken1 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri1 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the default Token Endpoint Auth
     * Method <code>client_secret_basic</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "omittedTokenEndpointAuthMethodStep1")
    public void omittedTokenEndpointAuthMethodStep2(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath + "?"
                + registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken1);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedTokenEndpointAuthMethodStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
            assertEquals(jsonObj.getString(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                    AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
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
     * Register a client with Token Endpoint Auth Method
     * <code>client_secret_basic</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasicStep1(final String registerPath, final String redirectUris)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretBasicStep1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
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
            clientSecret2 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken2 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri2 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Token Endpoint Auth Method
     * <code>client_secret_basic</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep1")
    public void tokenEndpointAuthMethodClientSecretBasicStep2(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath + "?"
                + registrationClientUri2.substring(registrationClientUri2.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken2);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretBasicStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
            assertEquals(jsonObj.getString(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                    AuthenticationMethod.CLIENT_SECRET_BASIC.toString());
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
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
     * Request authorization code.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep2")
    public void tokenEndpointAuthMethodClientSecretBasicStep3(final String authorizePath, final String userId,
                                                              final String userSecret, final String redirectUri) throws Exception {
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        List<String> scopes = new ArrayList<String>();
        scopes.add("openid");
        scopes.add("profile");
        scopes.add("address");
        scopes.add("email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId2, scopes,
                redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretBasicStep3", response, entity);

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

    /**
     * Call to Token Endpoint with Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"tokenEndpointAuthMethodClientSecretBasicStep3"})
    public void tokenEndpointAuthMethodClientSecretBasicStep4(final String tokenPath, final String redirectUri)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setCode(authorizationCode2);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId2);
        tokenRequest.setAuthPassword(clientSecret2);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretBasicStep4", response, entity);

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
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method
     * <code>client_secret_post</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep2")
    public void tokenEndpointAuthMethodClientSecretBasicFail1(final String tokenPath, final String userId,
                                                              final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId2);
        tokenRequest.setAuthPassword(clientSecret2);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretBasicFail1", response, entity);

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
     * Fail 2: Call to Token Endpoint with Auth Method
     * <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "audience", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep2")
    public void tokenEndpointAuthMethodClientSecretBasicFail2(final String tokenPath, final String audience,
                                                              final String userId, final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAudience(audience);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId2);
        tokenRequest.setAuthPassword(clientSecret2);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretBasicFail2", response, entity);

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
     * Fail 3: Call to Token Endpoint with Auth Method
     * <code>private_key_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret", "audience", "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep2")
    public void tokenEndpointAuthMethodClientSecretBasicFail3(final String tokenPath, final String userId,
                                                              final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                              final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId2);
        tokenRequest.setAuthPassword(clientSecret2);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretBasicFail3", response, entity);

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
     * Register a client with Token Endpoint Auth Method
     * <code>client_secret_post</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPostStep1(final String registerPath, final String redirectUris)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretPostStep1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
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
            clientSecret3 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken3 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri3 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Token Endpoint Auth Method
     * <code>client_secret_post</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep1")
    public void tokenEndpointAuthMethodClientSecretPostStep2(final String registerPath) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath + "?"
                + registrationClientUri3.substring(registrationClientUri3.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken3);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretPostStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
            assertEquals(jsonObj.getString(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                    AuthenticationMethod.CLIENT_SECRET_POST.toString());
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
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
     * Request authorization code.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep2")
    public void tokenEndpointAuthMethodClientSecretPostStep3(final String authorizePath, final String userId,
                                                             final String userSecret, final String redirectUri) throws Exception {
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        List<String> scopes = new ArrayList<String>();
        scopes.add("openid");
        scopes.add("profile");
        scopes.add("address");
        scopes.add("email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId3, scopes,
                redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretPostStep3", response, entity);

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

                authorizationCode3 = params.get(AuthorizeResponseParam.CODE);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    /**
     * Call to Token Endpoint with Auth Method <code>client_secret_post</code>.
     */
    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"tokenEndpointAuthMethodClientSecretPostStep3"})
    public void tokenEndpointAuthMethodClientSecretPostStep4(final String tokenPath, final String redirectUri)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.setCode(authorizationCode3);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId3);
        tokenRequest.setAuthPassword(clientSecret3);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretBasicStep4", response, entity);

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
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method
     * <code>client_secret_basic</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep2")
    public void tokenEndpointAuthMethodClientSecretPostFail1(final String tokenPath, final String userId,
                                                             final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId3);
        tokenRequest.setAuthPassword(clientSecret3);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretPostFail1", response, entity);

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
     * Fail 2: Call to Token Endpoint with Auth Method
     * <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "audience", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep2")
    public void tokenEndpointAuthMethodClientSecretPostFail2(final String tokenPath, final String audience,
                                                             final String userId, final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAudience(audience);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId3);
        tokenRequest.setAuthPassword(clientSecret3);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretPostFail2", response, entity);

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
     * Fail 3: Call to Token Endpoint with Auth Method
     * <code>private_key_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret", "audience", "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep2")
    public void tokenEndpointAuthMethodClientSecretPostFail3(final String tokenPath, final String userId,
                                                             final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                             final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId3);
        tokenRequest.setAuthPassword(clientSecret3);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretPostFail3", response, entity);

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
     * Register a client with Token Endpoint Auth Method
     * <code>client_secret_jwt</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtStep1(final String registerPath, final String redirectUris)
            throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretJwtStep1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
            assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            clientId4 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret4 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken4 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri4 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Token Endpoint Auth Method
     * <code>client_secret_jwt</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep1")
    public void tokenEndpointAuthMethodClientSecretJwtStep2(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath + "?"
                + registrationClientUri4.substring(registrationClientUri4.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken4);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretJwtStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
            assertEquals(jsonObj.getString(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                    AuthenticationMethod.CLIENT_SECRET_JWT.toString());
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
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
     * Request authorization code.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep2")
    public void tokenEndpointAuthMethodClientSecretJwtStep3(final String authorizePath, final String userId,
                                                            final String userSecret, final String redirectUri) throws Exception {
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        List<String> scopes = new ArrayList<String>();
        scopes.add("openid");
        scopes.add("profile");
        scopes.add("address");
        scopes.add("email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId4, scopes,
                redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();

        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretJwtStep3", response, entity);

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

    /**
     * Call to Token Endpoint with Auth Method <code>client_secret_Jwt</code>.
     */
    @Parameters({"tokenPath", "redirectUri", "audience", "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = {"tokenEndpointAuthMethodClientSecretJwtStep3"})
    public void tokenEndpointAuthMethodClientSecretJwtStep4(final String tokenPath, final String redirectUri,
                                                            final String audience, final String keyId, final String dnName, final String keyStoreFile,
                                                            final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setAudience(audience);
        tokenRequest.setCode(authorizationCode4);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId4);
        tokenRequest.setAuthPassword(clientSecret4);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretJwtStep4", response, entity);

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
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method
     * <code>client_secret_basic</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep2")
    public void tokenEndpointAuthMethodClientSecretJwtFail1(final String tokenPath, final String userId,
                                                            final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId4);
        tokenRequest.setAuthPassword(clientSecret4);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretJwtFail1", response, entity);

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
     * Fail 2: Call to Token Endpoint with Auth Method
     * <code>client_secret_post</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep2")
    public void tokenEndpointAuthMethodClientSecretJwtFail2(final String tokenPath, final String userId,
                                                            final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId4);
        tokenRequest.setAuthPassword(clientSecret4);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretJwtFail2", response, entity);

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
     * Fail 3: Call to Token Endpoint with Auth Method
     * <code>private_key_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret", "audience", "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep2")
    public void tokenEndpointAuthMethodClientSecretJwtFail3(final String tokenPath, final String userId,
                                                            final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                            final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId4);
        tokenRequest.setAuthPassword(clientSecret4);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodClientSecretJwtFail3", response, entity);

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
     * Register a client with Token Endpoint Auth Method
     * <code>private_key_jwt</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtStep1(final String registerPath, final String redirectUris,
                                                          final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        String registerRequestContent = registerRequest.getJSONParameters().toString(4);

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodPrivateKeyJwtStep1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
            assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            clientId5 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret5 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken5 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri5 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Token Endpoint Auth Method
     * <code>private_key_jwt</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep1")
    public void tokenEndpointAuthMethodPrivateKeyJwtStep2(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath + "?"
                + registrationClientUri5.substring(registrationClientUri5.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken5);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodPrivateKeyJwtStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(TOKEN_ENDPOINT_AUTH_METHOD.toString()));
            assertEquals(jsonObj.getString(TOKEN_ENDPOINT_AUTH_METHOD.toString()),
                    AuthenticationMethod.PRIVATE_KEY_JWT.toString());
            assertTrue(jsonObj.has(APPLICATION_TYPE.toString()));
            assertTrue(jsonObj.has(RESPONSE_TYPES.toString()));
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
     * Request authorization code.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep2")
    public void tokenEndpointAuthMethodPrivateKeyJwtStep3(final String authorizePath, final String userId,
                                                          final String userSecret, final String redirectUri) throws Exception {
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        List<String> scopes = new ArrayList<String>();
        scopes.add("openid");
        scopes.add("profile");
        scopes.add("address");
        scopes.add("email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId5, scopes,
                redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodPrivateKeyJwtStep3", response, entity);

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

                authorizationCode5 = params.get(AuthorizeResponseParam.CODE);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    /**
     * Call to Token Endpoint with Auth Method <code>private_key_jwt</code>.
     */
    @Parameters({"tokenPath", "redirectUri", "audience", "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = {"tokenEndpointAuthMethodPrivateKeyJwtStep3"})
    public void tokenEndpointAuthMethodPrivateKeyJwtStep4(final String tokenPath, final String redirectUri,
                                                          final String audience, final String keyId, final String keyStoreFile, final String keyStoreSecret)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.RS256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);
        tokenRequest.setCode(authorizationCode5);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId5);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodPrivateKeyJwtStep4", response, entity);

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
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method
     * <code>client_secret_basic</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep2")
    public void tokenEndpointAuthMethodPrivateKeyJwtFail1(final String tokenPath, final String userId,
                                                          final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId5);
        tokenRequest.setAuthPassword(clientSecret5);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodPrivateKeyJwtFail1", response, entity);

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
     * Fail 2: Call to Token Endpoint with Auth Method
     * <code>client_secret_post</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep2")
    public void tokenEndpointAuthMethodPrivateKeyJwtFail2(final String tokenPath, final String userId,
                                                          final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId5);
        tokenRequest.setAuthPassword(clientSecret5);

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));

        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodPrivateKeyJwtFail2", response, entity);

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
     * Fail 3: Call to Token Endpoint with Auth Method
     * <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "audience", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep2")
    public void tokenEndpointAuthMethodPrivateKeyJwtFail3(final String tokenPath, final String audience,
                                                          final String userId, final String userSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAudience(audience);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId5);
        tokenRequest.setAuthPassword(clientSecret5);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("tokenEndpointAuthMethodPrivateKeyJwtFail3", response, entity);

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

}

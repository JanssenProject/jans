/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.RegisterRequest;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
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
import java.util.Arrays;
import java.util.List;

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
 * Functional tests for Token Web Services (embedded)
 *
 * @author Javier Rojas Blum
 * @version March 9, 2019
 */
public class TokenRestWebServiceWithESAlgEmbeddedTest extends BaseTest {

    private static String clientId1;
    private static String clientSecret1;
    private static String clientId2;
    private static String clientSecret2;
    private static String clientId3;
    private static String clientSecret3;
    private static String clientId4;
    private static String clientSecret4;
    private static String clientId5;
    private static String clientSecret5;
    private static String clientId6;
    private static String clientSecret6;
    @ArquillianResource
    private URI url;

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenWithClientSecretJwtES256Step1(final String registerPath, final String redirectUris,
                                                                final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES256Step1", response, entity);

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

    @Parameters({"tokenPath", "userId", "userSecret", "audience", "ES256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestAccessTokenWithClientSecretJwtES256Step1")
    public void requestAccessTokenWithClientSecretJwtES256Step2(final String tokenPath, final String userId,
                                                                final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                                final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId1);
        tokenRequest.setAuthPassword(clientSecret1);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES256Step2", response, entity);

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
        }
    }

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenWithClientSecretJwtES384Step1(final String registerPath, final String redirectUris,
                                                                final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES384Step1", response, entity);

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

    @Parameters({"tokenPath", "userId", "userSecret", "audience", "ES384_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestAccessTokenWithClientSecretJwtES384Step1")
    public void requestAccessTokenWithClientSecretJwtES384Step2(final String tokenPath, final String userId,
                                                                final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                                final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId2);
        tokenRequest.setAuthPassword(clientSecret2);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES384Step2", response, entity);

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
        }
    }

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenWithClientSecretJwtES512Step1(final String registerPath, final String redirectUris,
                                                                final String jwksUri) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES384Step1", response, entity);

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

    @Parameters({"tokenPath", "userId", "userSecret", "audience", "ES512_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestAccessTokenWithClientSecretJwtES512Step1")
    public void requestAccessTokenWithClientSecretJwtES512Step2(final String tokenPath, final String userId,
                                                                final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                                final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId3);
        tokenRequest.setAuthPassword(clientSecret3);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES512Step2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString(Constants.PRAGMA) != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);
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
    public void requestAccessTokenWithClientSecretJwtES256X509CertStep1(final String registerPath,
                                                                        final String redirectUris, final String jwksUri) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));

        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES256X509CertStep1", response, entity);

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

    @Parameters({"tokenPath", "userId", "userSecret", "audience", "ES256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestAccessTokenWithClientSecretJwtES256X509CertStep1")
    public void requestAccessTokenWithClientSecretJwtES256X509CertStep2(final String tokenPath, final String userId,
                                                                        final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                                        final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId4);
        tokenRequest.setAuthPassword(clientSecret4);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES256);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES256X509CertStep2", response, entity);

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
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenWithClientSecretJwtES384X509CertStep1(final String registerPath,
                                                                        final String redirectUris, final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES384X509CertStep1", response, entity);

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

            clientId5 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret5 = jsonObj.getString(CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"tokenPath", "userId", "userSecret", "audience", "ES384_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestAccessTokenWithClientSecretJwtES384X509CertStep1")
    public void requestAccessTokenWithClientSecretJwtES384X509CertStep2(final String tokenPath, final String userId,
                                                                        final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                                        final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId5);
        tokenRequest.setAuthPassword(clientSecret5);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES384);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));

        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES384X509CertStep2", response, entity);

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
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestAccessTokenWithClientSecretJwtES512X509CertStep1(final String registerPath,
                                                                        final String redirectUris, final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES512X509CertStep1", response, entity);

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

            clientId6 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret6 = jsonObj.getString(CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"tokenPath", "userId", "userSecret", "audience", "ES512_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestAccessTokenWithClientSecretJwtES512X509CertStep1")
    public void requestAccessTokenWithClientSecretJwtES512X509CertStep2(final String tokenPath, final String userId,
                                                                        final String userSecret, final String audience, final String keyId, final String keyStoreFile,
                                                                        final String keyStoreSecret) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthUsername(clientId6);
        tokenRequest.setAuthPassword(clientSecret6);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAlgorithm(SignatureAlgorithm.ES512);
        tokenRequest.setKeyId(keyId);
        tokenRequest.setCryptoProvider(cryptoProvider);
        tokenRequest.setAudience(audience);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwtES512X509CertStep2", response, entity);

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
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

}

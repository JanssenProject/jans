/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
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

import static io.jans.as.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static io.jans.as.model.register.RegisterRequestParam.CLIENT_NAME;
import static io.jans.as.model.register.RegisterRequestParam.ID_TOKEN_SIGNED_RESPONSE_ALG;
import static io.jans.as.model.register.RegisterRequestParam.REDIRECT_URIS;
import static io.jans.as.model.register.RegisterRequestParam.REQUEST_OBJECT_SIGNING_ALG;
import static io.jans.as.model.register.RegisterRequestParam.RESPONSE_TYPES;
import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_ID_ISSUED_AT;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_SECRET;
import static io.jans.as.model.register.RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT;
import static io.jans.as.model.register.RegisterResponseParam.REGISTRATION_CLIENT_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class RequestObjectSigningAlgRestrictionEmbeddedTest extends BaseTest {

    public static final String ACR_VALUE = "basic";
    private static String clientId1;
    private static String clientSecret1;
    private static String registrationAccessToken1;
    private static String registrationClientUri1;
    private static String clientId2;
    private static String clientSecret2;
    private static String registrationAccessToken2;
    private static String registrationClientUri2;
    private static String clientId3;
    private static String clientSecret3;
    private static String registrationAccessToken3;
    private static String clientId4;
    private static String clientSecret4;
    private static String registrationAccessToken4;
    private static String clientId5;
    private static String clientSecret5;
    private static String registrationAccessToken5;
    private static String clientId6;
    private static String clientSecret6;
    private static String registrationAccessToken6;
    private static String clientId7;
    private static String clientSecret7;
    private static String registrationAccessToken7;
    private static String clientId8;
    private static String clientSecret8;
    private static String registrationAccessToken8;
    private static String clientId9;
    private static String clientSecret9;
    private static String registrationAccessToken9;
    private static String clientId10;
    private static String clientSecret10;
    private static String registrationAccessToken10;
    private static String clientId11;
    private static String clientSecret11;
    private static String registrationAccessToken11;
    @ArquillianResource
    private URI url;

    /**
     * Register a client without specify a Request Object Signing Alg.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void omittedRequestObjectSigningAlgStep1(final String registerPath, final String redirectUris,
                                                    final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep1", response, entity);

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

            clientId1 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret1 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken1 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
            registrationClientUri1 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the default Request Object
     * Signing Alg <code>null</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep1")
    public void omittedRequestObjectSigningAlgStep2(final String registerPath) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(null);

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1)).request();
        request.header("Authorization", "Bearer " + registrationAccessToken1);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertFalse(jsonObj.has(REQUEST_OBJECT_SIGNING_ALG.toString()));
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
     * Request authorization with Request Object Signing Alg <code>NONE</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3NONE(final String authorizePath, final String userId,
                                                        final String userSecret, final String redirectUri) throws Exception {
        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.NONE, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3NONE", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("id_token"), "The idToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>HS256</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3HS256(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri) throws Exception {
        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS256, clientSecret1, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3HS256", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("id_token"), "The idToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>HS384</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3HS384(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri) throws Exception {
        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS384, clientSecret1, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3HS384", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("id_token"), "The idToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>HS512</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3HS512(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri) throws Exception {
        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS512, clientSecret1, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3HS512", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("id_token"), "The idToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>RS256</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "RS256_keyId", "dnName", "keyStoreFile",
            "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3RS256(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri, final String keyId, final String dnName,
                                                         final String keyStoreFile, final String keyStoreSecret) throws Exception {
        Builder request = null;
        try {
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.RS256, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3RS256", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>RS384</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "RS384_keyId", "dnName", "keyStoreFile",
            "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3RS384(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri, final String keyId, final String dnName,
                                                         final String keyStoreFile, final String keyStoreSecret) throws Exception {
        Builder request = null;
        try {
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.RS384, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3RS384", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>RS512</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "RS512_keyId", "dnName", "keyStoreFile",
            "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3RS512(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri, final String keyId, final String dnName,
                                                         final String keyStoreFile, final String keyStoreSecret) throws Exception {
        Builder request = null;
        try {
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.RS512, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3RS512", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>ES256</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "ES256_keyId", "dnName", "keyStoreFile",
            "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3ES256(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri, final String keyId, final String dnName,
                                                         final String keyStoreFile, final String keyStoreSecret) throws Exception {
        Builder request = null;
        try {
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.ES256, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3ES256", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>ES384</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "ES384_keyId", "dnName", "keyStoreFile",
            "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3ES384(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri, final String keyId, final String dnName,
                                                         final String keyStoreFile, final String keyStoreSecret) throws Exception {
        Builder request = null;
        try {
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.ES384, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3ES384", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Request authorization with Request Object Signing Alg <code>ES512</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "ES512_keyId", "dnName", "keyStoreFile",
            "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3ES512(final String authorizePath, final String userId,
                                                         final String userSecret, final String redirectUri, final String keyId, final String dnName,
                                                         final String keyStoreFile, final String keyStoreSecret) throws Exception {
        Builder request = null;
        try {
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId1, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.ES512, cryptoProvider);
            jwtAuthorizationRequest.setKeyId(keyId);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("omittedRequestObjectSigningAlgStep3ES512", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Register a client with Request Object Signing Alg <code>NONE</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgNoneStep1(final String registerPath, final String redirectUris,
                                                 final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.NONE);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgNoneStep1", response, entity);

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
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>NONE</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "requestObjectSigningAlgNoneStep1")
    public void requestObjectSigningAlgNoneStep2(final String registerPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath + "?"
                + registrationClientUri2.substring(registrationClientUri2.indexOf("?") + 1)).request();

        request.header("Authorization", "Bearer " + registrationAccessToken2);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgNoneStep2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            // Registered Metadata
            assertTrue(jsonObj.has(REQUEST_OBJECT_SIGNING_ALG.toString()));
            assertEquals(SignatureAlgorithm.fromString(jsonObj.getString(REQUEST_OBJECT_SIGNING_ALG.toString())),
                    SignatureAlgorithm.NONE);
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
     * Request authorization with Request Object Signing Alg <code>NONE</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestObjectSigningAlgNoneStep2")
    public void requestObjectSigningAlgNoneStep3(final String authorizePath, final String userId,
                                                 final String userSecret, final String redirectUri) throws Exception {
        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId2, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.NONE, cryptoProvider);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest
                    .addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest
                    .addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE,
                    ClaimValue.createValueList(new String[]{ACR_VALUE})));
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);
            System.out.println("Request JWT: " + authJwt);

            request = ResteasyClientBuilder.newClient()
                    .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
            request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
            request.header("Accept", MediaType.TEXT_PLAIN);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgNoneStep3", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("id_token"), "The idToken is null");
            assertNotNull(params.get("scope"), "The scope is null");
            assertNotNull(params.get("state"), "The state is null");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>HS256</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestObjectSigningAlgHS256Step1(final String registerPath, final String redirectUris)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS256);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgHS256Step1", response, entity);

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

            clientId3 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret3 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken3 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>HS384</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestObjectSigningAlgHS384Step1(final String registerPath, final String redirectUris)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS384);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgHS384Step1", response, entity);

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

            clientId4 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret4 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken4 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>HS512</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestObjectSigningAlgHS512Step1(final String registerPath, final String redirectUris)
            throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS512);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgHS512Step1", response, entity);

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

            clientId5 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret5 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken5 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>RS256</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgRS256Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgRS256Step1", response, entity);

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

            clientId6 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret6 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken6 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>RS384</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgRS384Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgRS384Step1", response, entity);

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

            clientId7 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret7 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken7 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>RS512</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgRS512Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgRS512Step1", response, entity);

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

            clientId8 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret8 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken8 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>ES256</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgES256Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgES256Step1", response, entity);

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

            clientId9 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret9 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken9 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>ES384</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgES384Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
            registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestObjectSigningAlgES256Step1", response, entity);

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

            clientId10 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret10 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
            registrationAccessToken10 = jsonObj.getString(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>ES384</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Read client to check whether it is using the Request Object Signing Alg
     * <code>ES512</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg
     * <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg
     * <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg
     * <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg
     * <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg
     * <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg
     * <code>RS384</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg
     * <code>RS512</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg
     * <code>ES256</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg
     * <code>ES384</code>.
     */
}
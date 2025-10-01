/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.RegisterRequest;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterResponseParam;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ResponseAsserter;
import io.jans.as.server.util.ServerUtil;
import io.jans.as.server.util.TestUtil;
import io.jans.util.StringHelper;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.jans.as.model.register.RegisterResponseParam.*;
import static org.testng.Assert.*;

/**
 * Functional tests for OpenID Request Object (embedded)
 *
 * @author Javier Rojas Blum
 * @version December 12, 2016
 */
public class OpenIDRequestObjectEmbeddedTest extends BaseTest {

    public static final String ACR_VALUE = "basic";
    private static String clientId;
    private static String clientSecret;
    private static String accessToken1;
    private static String accessToken2;
    private static String clientId1;
    private static String clientSecret1;
    private static String clientId2;
    private static String clientSecret2;
    private static String clientId3;
    @ArquillianResource
    private URI url;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.TOKEN,
                    ResponseType.ID_TOKEN);

            io.jans.as.client.RegisterRequest registerRequest = new io.jans.as.client.RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

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

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestParameterMethod1Step1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS256);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestParameterMethod1Step1", response, entity);

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

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestParameterMethod1Step1")
    public void requestParameterMethod1Step2(final String authorizePath, final String userId, final String userSecret,
                                             final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();

            io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId1, scopes,
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

        showResponse("requestParameterMethod1Step2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The accessToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.ID_TOKEN), "The idToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
            assertEquals(params.get(AuthorizeResponseParam.STATE), state);

            accessToken1 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = {"requestParameterMethod1Step2"})
    public void requestParameterMethodUserInfo(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(accessToken1);
        userInfoRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestParameterMethodUserInfo", response, entity);

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
            assertTrue(jsonObj.has(JwtClaimName.SUBJECT_IDENTIFIER));
            assertTrue(jsonObj.has(JwtClaimName.NAME));
            assertTrue(jsonObj.has(JwtClaimName.GIVEN_NAME));
            assertTrue(jsonObj.has(JwtClaimName.FAMILY_NAME));
            assertTrue(jsonObj.has(JwtClaimName.EMAIL));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestParameterMethod2Step1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS256);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestParameterMethod2Step1", response, entity);

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

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestParameterMethod2Step1")
    public void requestParameterMethod2Step2(final String authorizePath, final String userId, final String userSecret,
                                             final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile");
            String nonce = UUID.randomUUID().toString();

            io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId2, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS256, clientSecret2, cryptoProvider);
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim("name", ClaimValue.createEssential(true)));
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

        showResponse("requestParameterMethod2Step2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The accessToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
            assertEquals(params.get(AuthorizeResponseParam.STATE), state);

            accessToken2 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = {"requestParameterMethod2Step2"})
    public void requestParameterMethod2Step3(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(accessToken2);
        userInfoRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestParameterMethod2Step3", response, entity);

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
            assertTrue(jsonObj.has(JwtClaimName.SUBJECT_IDENTIFIER));
            assertTrue(jsonObj.has(JwtClaimName.NAME));
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestParameterMethodFail1(final String authorizePath, final String userId, final String userSecret,
                                            final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setRequest("INVALID_REQUEST_OBJECT");
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestParameterMethodFail1 (Invalid OpenID Request Object)", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get("error"), "The error value is null");
                assertNotNull(params.get("error_description"), "The errorDescription value is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            }
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestParameterMethodFail2(final String authorizePath, final String userId, final String userSecret,
                                            final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        Builder request = null;
        try {

            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();

            io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
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
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt + "INVALID_SIGNATURE");
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

        showResponse("requestParameterMethodFail2 (Invalid OpenID Request Object, due to invalid signature)", response,
                entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get("error"), "The error value is null");
                assertNotNull(params.get("error_description"), "The errorDescription value is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            }
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestParameterMethodFail3(final String authorizePath, final String userId, final String userSecret,
                                            final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();

            io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
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
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            jwtAuthorizationRequest.setClientId("INVALID_CLIENT_ID");
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

        showResponse("requestParameterMethodFail3 (Invalid OpenID Request Object)", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get("error"), "The error value is null");
                assertNotNull(params.get("error_description"), "The errorDescription value is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            }
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestParameterMethodFail4(final String authorizePath, final String userId, final String userSecret,
                                            final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();

            io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
            jwtAuthorizationRequest.addIdTokenClaim(
                    new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue("INVALID_USER_ID")));
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

        showResponse("requestParameterMethodFail4", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get("error"), "The error value is null");
                assertNotNull(params.get("error_description"), "The errorDescription value is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            }
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestParameterMethodWithMaxAgeRestriction(final String authorizePath, final String userId,
                                                            final String userSecret, final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();

            io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                    redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.getPrompts().add(Prompt.NONE);
            authorizationRequest.setAuthUsername(userId);
            authorizationRequest.setAuthPassword(userSecret);

            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
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
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
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

        showResponse("requestParameterMethodWithMaxAgeRestriction", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The accessToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.ID_TOKEN), "The idToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
            assertEquals(params.get(AuthorizeResponseParam.STATE), state);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "requestFileBasePath", "requestFileBaseUrl"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    // This tests requires a place to publish a request object via HTTPS
    public void requestFileMethod(final String authorizePath, final String userId, final String userSecret,
                                  final String redirectUri, @Optional final String requestFileBasePath,
                                  @Optional final String requestFileBaseUrl) throws Exception {
        if (StringHelper.isEmpty(requestFileBasePath) || StringHelper.isEmpty(requestFileBaseUrl)) {
            return;
        }

        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        try {
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
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
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            String hash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(authJwt));
            String fileName = UUID.randomUUID().toString() + ".txt";
            String filePath = requestFileBasePath + File.separator + fileName;
            String fileUrl = requestFileBaseUrl + "/" + fileName + "#" + hash;
            FileWriter fw = new FileWriter(filePath);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(authJwt);
            bw.close();
            fw.close();
            authorizationRequest.setRequestUri(fileUrl);
            System.out.println("Request JWT: " + authJwt);
            System.out.println("Request File Path: " + filePath);
            System.out.println("Request File URL: " + fileUrl);
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestFileMethod", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get("access_token"), "The accessToken is null");
            assertNotNull(params.get("id_token"), "The idToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
            assertEquals(params.get(AuthorizeResponseParam.STATE), state);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestFileMethodFail1(final String authorizePath, final String userId, final String userSecret,
                                       final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        authorizationRequest.setRequest("FAKE_REQUEST");
        authorizationRequest.setRequestUri("FAKE_REQUEST_URI");

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestFileMethodFail1", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get("error"), "The error value is null");
                assertNotNull(params.get("error_description"), "The errorDescription value is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            }
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "requestFileBaseUrl"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestFileMethodFail2(final String authorizePath, final String userId, final String userSecret,
                                       final String redirectUri, @Optional final String requestFileBaseUrl) throws Exception {
        if (StringHelper.isEmpty(requestFileBaseUrl)) {
            return;
        }

        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        authorizationRequest.setRequestUri(requestFileBaseUrl + "/FAKE_REQUEST_URI");

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestFileMethodFail2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get("error"), "The error value is null");
                assertNotNull(params.get("error_description"), "The errorDescription value is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            }
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri", "requestFileBasePath", "requestFileBaseUrl"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    // This test requires a place to publish a request object via HTTPS
    public void requestFileMethodFail3(final String authorizePath, final String userId, final String userSecret,
                                       final String redirectUri, @Optional final String requestFileBasePath,
                                       @Optional final String requestFileBaseUrl) throws Exception {
        if (StringHelper.isEmpty(requestFileBasePath) || StringHelper.isEmpty(requestFileBaseUrl)) {
            return;
        }

        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        try {
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                    SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
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
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            String hash = "INVALID_HASH";
            String fileName = UUID.randomUUID().toString() + ".txt";
            String filePath = requestFileBasePath + File.separator + fileName;
            String fileUrl = requestFileBaseUrl + "/" + fileName + "#" + hash;
            FileWriter fw = new FileWriter(filePath);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(authJwt);
            bw.close();
            fw.close();
            authorizationRequest.setRequestUri(fileUrl);
            System.out.println("Request JWT: " + authJwt);
            System.out.println("Request File Path: " + filePath);
            System.out.println("Request File URL: " + fileUrl);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage(), e);
        }

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestFileMethodFail3", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get("error"), "The error value is null");
                assertNotNull(params.get("error_description"), "The errorDescription value is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), state);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            }
        }
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestParameterMethodAlgNoneStep1(final String registerPath, final String redirectUris)
            throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

            io.jans.as.client.RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.NONE);
            registerRequest.addCustomAttribute("jansTrustedClnt", "true");

            registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestParameterMethodAlgNoneStep1", response, entity);

        ResponseAsserter responseAsserter = ResponseAsserter.of(response.getStatus(), entity);
        responseAsserter.assertRegisterResponse();
        clientId3 = responseAsserter.getJson().getJson().getString(RegisterResponseParam.CLIENT_ID.toString());
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestParameterMethodAlgNoneStep1")
    public void requestParameterMethodAlgNoneStep2(final String authorizePath, final String userId,
                                                   final String userSecret, final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        Builder request = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
            List<String> scopes = Arrays.asList("openid");
            String nonce = UUID.randomUUID().toString();

            io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId3, scopes,
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

        showResponse("requestParameterMethodAlgNoneStep2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = io.jans.as.model.util.QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The accessToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
            assertEquals(params.get(AuthorizeResponseParam.STATE), state);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

}
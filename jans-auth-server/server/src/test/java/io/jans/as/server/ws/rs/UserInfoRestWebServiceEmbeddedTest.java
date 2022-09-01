/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.UserInfoRequest;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.server.util.TestUtil;
import io.jans.as.model.authorize.AuthorizeResponseParam;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.register.RegisterResponseParam;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.codec.binary.Base64;
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Functional tests for User Info Web Services (embedded)
 *
 * @author Javier Rojas Blum
 * @version May 14, 2019
 */
public class UserInfoRestWebServiceEmbeddedTest extends BaseTest {

    private static String clientId;
    private static String clientSecret;
    private static String accessToken1;
    private static String accessToken3;
    private static String accessToken4;
    private static String accessToken5;
    private static String accessToken6;
    private static String accessToken7;
    private static String clientId1;
    private static String clientId2;
    private static String clientId3;
    private static String clientSecret1;
    private static String clientSecret2;
    private static String clientSecret3;
    @ArquillianResource
    private URI url;

    @Parameters({"registerPath", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris,
                                          final String sectorIdentifierUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(io.jans.as.model.common.SubjectType.PAIRWISE);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");
        registerRequest.setClaims(Arrays.asList("o"));

        List<GrantType> grantTypes = Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
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
            fail(e.getMessage(), e);
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestUserInfoStep1ImplicitFlow(final String authorizePath, final String userId,
                                                 final String userSecret, final String redirectUri) throws Exception {
        final String userEncodedCredentials = Base64.encodeBase64String((userId + ":" + userSecret).getBytes());
        final String state = UUID.randomUUID().toString();

        List<io.jans.as.model.common.ResponseType> responseTypes = Arrays.asList(io.jans.as.model.common.ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(io.jans.as.model.common.Prompt.NONE);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + userEncodedCredentials);
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfo step 1 Implicit Flow", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The access token is null");
                assertNotNull(params.get(AuthorizeResponseParam.TOKEN_TYPE), "The token type is null");
                assertNotNull(params.get(AuthorizeResponseParam.EXPIRES_IN), "The expires in value is null");
                assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope must be null");
                assertNull(params.get("refresh_token"), "The refresh_token must be null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);

                accessToken1 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unexpected error");
            }
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoStep1ImplicitFlow")
    public void requestUserInfoStep2PostImplicitFlow(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();

        request.header("Authorization", "Bearer " + accessToken1);
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfo step 2 POST Implicit Flow", response, entity);

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

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoStep1ImplicitFlow")
    public void requestUserInfoStep2GetImplicitFlow(final String userInfoPath) throws Exception {
        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(null);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + userInfoPath + "?" + userInfoRequest.getQueryString()).request();
        request.header("Authorization", "Bearer " + accessToken1);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfo step 2 GET Implicit Flow", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
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

    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestUserInfoStep1PasswordFlow(final String tokenPath, final String userId, final String userSecret)
            throws Exception {
        // Testing with valid parameters
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + tokenPath).request();

        io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(io.jans.as.model.common.GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("openid profile address email");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoStep1PasswordFlow", response, entity);

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
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");

            accessToken4 = jsonObj.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoStep1PasswordFlow")
    public void requestUserInfoStep2PasswordFlow(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();
        request.header("Authorization", "Bearer " + accessToken4);
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoStep2PasswordFlow", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
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

    @Parameters({"userInfoPath"})
    @Test
    public void requestUserInfoInvalidRequest(final String userInfoPath) throws Exception {
        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(null);

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();
        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoInvalidRequest", response, entity);

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

    @Parameters({"userInfoPath"})
    @Test
    public void requestUserInfoInvalidToken(final String userInfoPath) throws Exception {
        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest("INVALID_ACCESS_TOKEN");
        userInfoRequest.setAuthorizationMethod(io.jans.as.model.common.AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();
        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoInvalidToken", response, entity);

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

    @Parameters({"userInfoPath"})
    @Test
    public void requestUserInfoInvalidSchema(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest("INVALID_ACCESS_TOKEN");

        Map<String, String> userInfoParameters = userInfoRequest.getParameters();
        userInfoParameters.put("schema", "INVALID_SCHEMA");

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoInvalidSchema", response, entity);

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

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestUserInfoAdditionalClaims(final String authorizePath, final String userId,
                                                final String userSecret, final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        List<io.jans.as.model.common.ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(io.jans.as.model.common.ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(io.jans.as.model.common.Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("invalid", ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("o", ClaimValue.createEssential(true)));

        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);
        System.out.println("Request JWT: " + authJwt);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoAdditionalClaims step 1", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        if (response.getLocation() != null) {
            try {
                URI uri = new URI(response.getLocation().toString());
                assertNotNull(uri.getFragment(), "Fragment is null");

                Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The access token is null");
                assertNotNull(params.get(AuthorizeResponseParam.TOKEN_TYPE), "The token type is null");
                assertNotNull(params.get(AuthorizeResponseParam.EXPIRES_IN), "The expires in value is null");
                assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope must be null");
                assertNull(params.get("refresh_token"), "The refresh_token must be null");
                assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                assertEquals(params.get(AuthorizeResponseParam.STATE), state);

                accessToken3 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoAdditionalClaims")
    public void requestUserInfoAdditionalClaimsStep2(final String userInfoPath) {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();

        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken3);
        userInfoRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoAdditionalClaims step 2", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(JwtClaimName.SUBJECT_IDENTIFIER));
            assertTrue(jsonObj.has(JwtClaimName.NAME));
            assertTrue(jsonObj.has(JwtClaimName.GIVEN_NAME));
            assertTrue(jsonObj.has(JwtClaimName.FAMILY_NAME));
            assertTrue(jsonObj.has(JwtClaimName.EMAIL));

            // Custom attributes
            assertTrue(jsonObj.has("o"));
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
    public void requestUserInfoHS256Step1(final String registerPath, final String redirectUris) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<GrantType> grantTypes = Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS256Step1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT.toString()));

            clientId1 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret1 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestUserInfoHS256Step1")
    public void requestUserInfoHS256Step2(final String authorizePath, final String userId, final String userSecret,
                                          final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        List<io.jans.as.model.common.ResponseType> responseTypes = Arrays.asList(io.jans.as.model.common.ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId1, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(io.jans.as.model.common.Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                SignatureAlgorithm.HS256, clientSecret1, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);
        System.out.println("Request JWT: " + authJwt);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS256Step2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The accessToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
            assertEquals(params.get(AuthorizeResponseParam.STATE), state);

            accessToken5 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoHS256Step2")
    public void requestUserInfoHS256Step3(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();
        request.header("Authorization", "Bearer " + accessToken5);
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS256Step3", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);

        try {
            Jwt jwt = Jwt.parse(entity);

            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.NAME));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EMAIL));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.PICTURE));
        } catch (InvalidJwtException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestUserInfoHS384Step1(final String registerPath, final String redirectUris) throws Exception {
        List<io.jans.as.model.common.ResponseType> responseTypes = Arrays.asList(io.jans.as.model.common.ResponseType.TOKEN);

        io.jans.as.client.RegisterRequest registerRequest = new io.jans.as.client.RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<io.jans.as.model.common.GrantType> grantTypes = Arrays.asList(
                io.jans.as.model.common.GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS384Step1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT.toString()));

            clientId2 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret2 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestUserInfoHS384Step1")
    public void requestUserInfoHS384Step2(final String authorizePath, final String userId, final String userSecret,
                                          final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        List<io.jans.as.model.common.ResponseType> responseTypes = Arrays.asList(io.jans.as.model.common.ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId2, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(io.jans.as.model.common.Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                SignatureAlgorithm.HS384, clientSecret2, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);
        System.out.println("Request JWT: " + authJwt);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();

        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS384Step2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The accessToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
            assertEquals(params.get(AuthorizeResponseParam.STATE), state);

            accessToken6 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoHS384Step2")
    public void requestUserInfoHS384Step3(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();
        request.header("Authorization", "Bearer " + accessToken6);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS384Step3", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);

        try {
            Jwt jwt = Jwt.parse(entity);

            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.NAME));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EMAIL));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.PICTURE));
        } catch (InvalidJwtException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestUserInfoHS512Step1(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + registerPath).request();

        List<io.jans.as.model.common.ResponseType> responseTypes = Arrays.asList(io.jans.as.model.common.ResponseType.TOKEN);

        io.jans.as.client.RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.addCustomAttribute("jansTrustedClnt", "true");

        List<io.jans.as.model.common.GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        registerRequest.setGrantTypes(grantTypes);

        String registerRequestContent = ServerUtil.toPrettyJson(registerRequest.getJSONParameters());

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS512Step1", response, entity);

        assertEquals(response.getStatus(), 201, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_SECRET_EXPIRES_AT.toString()));

            clientId3 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret3 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestUserInfoHS512Step1")
    public void requestUserInfoHS512Step2(final String authorizePath, final String userId, final String userSecret,
                                          final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        List<io.jans.as.model.common.ResponseType> responseTypes = Arrays.asList(io.jans.as.model.common.ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "email");
        String nonce = UUID.randomUUID().toString();

        io.jans.as.client.AuthorizationRequest authorizationRequest = new io.jans.as.client.AuthorizationRequest(responseTypes, clientId3, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(io.jans.as.model.common.Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                SignatureAlgorithm.HS512, clientSecret3, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);
        System.out.println("Request JWT: " + authJwt);

        Builder request = ResteasyClientBuilder.newClient()
                .target(getApiTagetURL(url) + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS512Step2", response, entity);

        assertEquals(response.getStatus(), 302, "Unexpected response code.");
        assertNotNull(response.getLocation(), "Unexpected result: " + response.getLocation());

        try {
            URI uri = new URI(response.getLocation().toString());
            assertNotNull(uri.getFragment(), "Query string is null");

            Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

            assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The accessToken is null");
            assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope is null");
            assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
            assertEquals(params.get(AuthorizeResponseParam.STATE), state);

            accessToken7 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Response URI is not well formed");
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoHS512Step2")
    public void requestUserInfoHS512Step3(final String userInfoPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + userInfoPath).request();

        request.header("Authorization", "Bearer " + accessToken7);
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        io.jans.as.client.UserInfoRequest userInfoRequest = new io.jans.as.client.UserInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestUserInfoHS512Step3", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals(Constants.NO_CACHE),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);

        try {
            Jwt jwt = Jwt.parse(entity);

            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.NAME));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EMAIL));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.PICTURE));
        } catch (InvalidJwtException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
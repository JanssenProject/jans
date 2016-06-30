/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.model.authorize.Claim;
import org.xdi.oxauth.client.model.authorize.ClaimValue;
import org.xdi.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version June 27, 2016
 */
public class RequestObjectSigningAlgRestrictionEmbeddedTest extends BaseTest {

    private String clientId1;
    private String clientSecret1;
    private String registrationAccessToken1;
    private String registrationClientUri1;

    private String clientId2;
    private String clientSecret2;
    private String registrationAccessToken2;
    private String registrationClientUri2;

    private String clientId3;
    private String clientSecret3;
    private String registrationAccessToken3;

    private String clientId4;
    private String clientSecret4;
    private String registrationAccessToken4;

    private String clientId5;
    private String clientSecret5;
    private String registrationAccessToken5;

    private String clientId6;
    private String clientSecret6;
    private String registrationAccessToken6;

    private String clientId7;
    private String clientSecret7;
    private String registrationAccessToken7;

    private String clientId8;
    private String clientSecret8;
    private String registrationAccessToken8;

    private String clientId9;
    private String clientSecret9;
    private String registrationAccessToken9;

    private String clientId10;
    private String clientSecret10;
    private String registrationAccessToken10;

    private String clientId11;
    private String clientSecret11;
    private String registrationAccessToken11;

    /**
     * Register a client without specify a Request Object Signing Alg.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void omittedRequestObjectSigningAlgStep1(final String registerPath, final String redirectUris,
                                                    final String jwksUri) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the default Request Object Signing Alg <code>null</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep1")
    public void omittedRequestObjectSigningAlgStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                RegisterRequest registerRequest = new RegisterRequest(null);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken1);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>NONE</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3NONE(final String authorizePath,
                                                        final String userId, final String userSecret,
                                                        final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.TOKEN,
                            ResponseType.ID_TOKEN);
                    List<String> scopes = Arrays.asList(
                            "openid",
                            "profile",
                            "address",
                            "email");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                            SignatureAlgorithm.NONE, cryptoProvider);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3NONE", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>HS256</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3HS256(final String authorizePath,
                                                         final String userId, final String userSecret,
                                                         final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.TOKEN,
                            ResponseType.ID_TOKEN);
                    List<String> scopes = Arrays.asList(
                            "openid",
                            "profile",
                            "address",
                            "email");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.HS256, clientSecret1, cryptoProvider);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3HS256", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>HS384</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3HS384(final String authorizePath,
                                                         final String userId, final String userSecret,
                                                         final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.TOKEN,
                            ResponseType.ID_TOKEN);
                    List<String> scopes = Arrays.asList(
                            "openid",
                            "profile",
                            "address",
                            "email");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.HS384, clientSecret1, cryptoProvider);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3HS384", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>HS512</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3HS512(final String authorizePath,
                                                         final String userId, final String userSecret,
                                                         final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.TOKEN,
                            ResponseType.ID_TOKEN);
                    List<String> scopes = Arrays.asList(
                            "openid",
                            "profile",
                            "address",
                            "email");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.HS512, clientSecret1, cryptoProvider);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3HS512", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>RS256</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3RS256(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.RS256, cryptoProvider);
                    jwtAuthorizationRequest.setKeyId(keyId);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3RS256", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>RS384</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3RS384(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.RS384, cryptoProvider);
                    jwtAuthorizationRequest.setKeyId(keyId);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3RS384", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>RS512</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3RS512(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.RS512, cryptoProvider);
                    jwtAuthorizationRequest.setKeyId(keyId);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3RS512", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>ES256</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3ES256(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.ES256, cryptoProvider);
                    jwtAuthorizationRequest.setKeyId(keyId);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3ES256", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>ES384</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "ES384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3ES384(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.ES384, cryptoProvider);
                    jwtAuthorizationRequest.setKeyId(keyId);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3ES384", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>ES512</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "ES512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "omittedRequestObjectSigningAlgStep2")
    public void omittedRequestObjectSigningAlgStep3ES512(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri,
            final String keyId, final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.ES512, cryptoProvider);
                    jwtAuthorizationRequest.setKeyId(keyId);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedRequestObjectSigningAlgStep3ES512", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Register a client with Request Object Signing Alg <code>NONE</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgNoneStep1(final String registerPath, final String redirectUris,
                                                 final String jwksUri) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.NONE);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgNoneStep1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>NONE</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "requestObjectSigningAlgNoneStep1")
    public void requestObjectSigningAlgNoneStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken2);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri2.substring(registrationClientUri2.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgNoneStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Request authorization with Request Object Signing Alg <code>NONE</code>.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestObjectSigningAlgNoneStep2")
    public void requestObjectSigningAlgNoneStep3(final String authorizePath,
                                                 final String userId, final String userSecret,
                                                 final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.TOKEN,
                            ResponseType.ID_TOKEN);
                    List<String> scopes = Arrays.asList(
                            "openid",
                            "profile",
                            "address",
                            "email");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId2, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                            SignatureAlgorithm.NONE, cryptoProvider);
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
                    String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                    authorizationRequest.setRequest(authJwt);
                    System.out.println("Request JWT: " + authJwt);

                    request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                    request.addHeader("Accept", MediaType.TEXT_PLAIN);
                    request.setQueryString(authorizationRequest.getQueryString());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgNoneStep3", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>HS256</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestObjectSigningAlgHS256Step1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS256);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgHS256Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>HS384</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestObjectSigningAlgHS384Step1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS384);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgHS384Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>HS512</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestObjectSigningAlgHS512Step1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS512);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgHS512Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>RS256</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgRS256Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgRS256Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>RS384</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgRS384Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgRS384Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>RS512</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgRS512Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgRS512Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>ES256</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgES256Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgES256Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>ES384</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestObjectSigningAlgES384Step1(final String registerPath, final String redirectUris,
                                                  final String jwksUri) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
                    registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestObjectSigningAlgES256Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>ES384</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Register a client with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Read client to check whether it is using the Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Request authorization with Request Object Signing Alg <code>ES512</code>.
     */

    /**
     * Fail 1: Request authorization with Request Object Signing Alg <code>NONE</code>.
     */

    /**
     * Fail 2: Request authorization with Request Object Signing Alg <code>HS256</code>.
     */

    /**
     * Fail 3: Request authorization with Request Object Signing Alg <code>HS384</code>.
     */

    /**
     * Fail 4: Request authorization with Request Object Signing Alg <code>HS512</code>.
     */

    /**
     * Fail 5: Request authorization with Request Object Signing Alg <code>RS256</code>.
     */

    /**
     * Fail 6: Request authorization with Request Object Signing Alg <code>RS384</code>.
     */

    /**
     * Fail 7: Request authorization with Request Object Signing Alg <code>RS512</code>.
     */

    /**
     * Fail 8: Request authorization with Request Object Signing Alg <code>ES256</code>.
     */

    /**
     * Fail 9: Request authorization with Request Object Signing Alg <code>ES384</code>.
     */
}
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
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;

/**
 * Functional tests for OpenID Request Object (embedded)
 *
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public class OpenIDRequestObjectWithHSAlgEmbeddedTest extends BaseTest {

    private String clientId1;
    private String clientSecret1;
    private String clientId2;
    private String clientSecret2;
    private String clientId3;
    private String clientSecret3;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestParameterMethodHS256Step1(final String registerPath, final String redirectUris) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS256);
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestParameterMethodHS256Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_ACCESS_TOKEN.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    clientId1 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                    clientSecret1 = jsonObj.getString(CLIENT_SECRET.toString());
                } catch (JSONException e) {
                    fail(e.getMessage(), e);
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestParameterMethodHS256Step1")
    public void requestParameterMethodHS256Step2(final String authorizePath,
                                                 final String userId, final String userSecret,
                                                 final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String state = "STATE0";
                    String nonce = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId1, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.HS256, clientSecret1, cryptoProvider);
                    jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue(userId)));
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
                showResponse("requestParameterMethodHS256Step2", response);

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
                    fail(e.getMessage(), e);
                }
            }
        }.run();
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestParameterMethodHS384Step1(final String registerPath, final String redirectUris) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS384);
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestParameterMethodHS384Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_ACCESS_TOKEN.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    clientId2 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                    clientSecret2 = jsonObj.getString(CLIENT_SECRET.toString());
                } catch (JSONException e) {
                    fail(e.getMessage(), e);
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestParameterMethodHS384Step1")
    public void requestParameterMethodHS384Step2(final String authorizePath,
                                                 final String userId, final String userSecret,
                                                 final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId2, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.HS384, clientSecret2, cryptoProvider);
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
                showResponse("requestParameterMethodHS384Step2", response);

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
                    fail(e.getMessage(), e);
                }
            }
        }.run();
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestParameterMethodHS512Step1(final String registerPath, final String redirectUris) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.HS512);
                    registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

                    request.setContentType(MediaType.APPLICATION_JSON);
                    String registerRequestContent = registerRequest.getJSONParameters().toString(4);
                    request.setContent(registerRequestContent.getBytes());
                } catch (JSONException e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestParameterMethodHS512Step1", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_ACCESS_TOKEN.toString()));
                    assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
                    assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
                    assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

                    clientId3 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                    clientSecret3 = jsonObj.getString(CLIENT_SECRET.toString());
                } catch (JSONException e) {
                    fail(e.getMessage(), e);
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestParameterMethodHS512Step1")
    public void requestParameterMethodHS512Step2(final String authorizePath,
                                                 final String userId, final String userSecret,
                                                 final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                    List<String> scopes = Arrays.asList("openid");
                    String nonce = UUID.randomUUID().toString();
                    String state = UUID.randomUUID().toString();

                    AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                            responseTypes, clientId3, scopes, redirectUri, nonce);
                    authorizationRequest.setState(state);
                    authorizationRequest.getPrompts().add(Prompt.NONE);
                    authorizationRequest.setAuthUsername(userId);
                    authorizationRequest.setAuthPassword(userSecret);

                    OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

                    JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                            authorizationRequest, SignatureAlgorithm.HS512, clientSecret3, cryptoProvider);
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
                showResponse("requestParameterMethodHS512Step2", response);

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
                    fail(e.getMessage(), e);
                }
            }
        }.run();
    }
}
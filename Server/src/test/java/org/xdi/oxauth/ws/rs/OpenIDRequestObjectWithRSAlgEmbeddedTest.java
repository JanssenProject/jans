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
public class OpenIDRequestObjectWithRSAlgEmbeddedTest extends BaseTest {

    private String clientId1;
    private String clientId2;
    private String clientId3;
    private String clientId4;
    private String clientId5;
    private String clientId6;

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestParameterMethodRS256Step1(final String registerPath, final String redirectUris,
                                                 final String jwksUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
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
                showResponse("requestParameterMethodRS256Step1", response);

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
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestParameterMethodRS256Step1")
    public void requestParameterMethodRS256Step2(
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
                showResponse("requestParameterMethodRS256Step2", response);

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

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestParameterMethodRS384Step1(final String registerPath, final String redirectUris,
                                                 final String jwksUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
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
                showResponse("requestParameterMethodRS384Step1", response);

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
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestParameterMethodRS384Step1")
    public void requestParameterMethodRS384Step2(
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
                            responseTypes, clientId2, scopes, redirectUri, nonce);
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
                showResponse("requestParameterMethodRS384Step2", response);

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

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestParameterMethodRS512Step1(final String registerPath, final String redirectUris,
                                                 final String jwksUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
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
                showResponse("requestParameterMethodRS512Step1", response);

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
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestParameterMethodRS512Step1")
    public void requestParameterMethodRS512Step2(
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
                            responseTypes, clientId3, scopes, redirectUri, nonce);
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
                showResponse("requestParameterMethodRS512Step2", response);

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

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestParameterMethodRS256X509CertStep1(final String registerPath, final String redirectUris,
                                                         final String jwksUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
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
                showResponse("requestParameterMethodRS256X509CertStep1", response);

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

                    clientId4 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestParameterMethodRS256X509CertStep1")
    public void requestParameterMethodRS256X509CertStep2(
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
                            responseTypes, clientId4, scopes, redirectUri, nonce);
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
                showResponse("requestParameterMethodRS256X509CertStep2", response);

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

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestParameterMethodRS384X509CertStep1(final String registerPath, final String redirectUris,
                                                         final String jwksUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
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
                showResponse("requestParameterMethodRS384X509CertStep1", response);

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

                    clientId5 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS384_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestParameterMethodRS384X509CertStep1")
    public void requestParameterMethodRS384X509CertStep2(
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
                            responseTypes, clientId5, scopes, redirectUri, nonce);
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
                showResponse("requestParameterMethodRS384X509CertStep2", response);

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

    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void requestParameterMethodRS512X509CertStep1(final String registerPath, final String redirectUris,
                                                         final String jwksUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setJwksUri(jwksUri);
                    registerRequest.setResponseTypes(responseTypes);
                    registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
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
                showResponse("requestParameterMethodRS512X509CertStep1", response);

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

                    clientId6 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri",
            "RS512_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "requestParameterMethodRS512X509CertStep1")
    public void requestParameterMethodRS512X509CertStep2(
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
                            responseTypes, clientId6, scopes, redirectUri, nonce);
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
                showResponse("requestParameterMethodRS512X509CertStep2", response);

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
}
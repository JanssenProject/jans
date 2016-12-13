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
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.model.authorize.AuthorizeResponseParam;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterRequestParam.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version December 12, 2016
 */
public class TokenEndpointAuthMethodRestrictionEmbeddedTest extends BaseTest {

    private String clientId1;
    private String registrationAccessToken1;
    private String registrationClientUri1;

    private String clientId2;
    private String clientSecret2;
    private String registrationAccessToken2;
    private String authorizationCode2;
    private String registrationClientUri2;

    private String clientId3;
    private String clientSecret3;
    private String registrationAccessToken3;
    private String authorizationCode3;
    private String registrationClientUri3;

    private String clientId4;
    private String clientSecret4;
    private String registrationAccessToken4;
    private String authorizationCode4;
    private String registrationClientUri4;

    private String clientId5;
    private String clientSecret5;
    private String registrationAccessToken5;
    private String authorizationCode5;
    private String registrationClientUri5;

    /**
     * Register a client without specify a Token Endpoint Auth Method.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void omittedTokenEndpointAuthMethodStep1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));

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
                showResponse("omittedTokenEndpointAuthMethodStep1", response);

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
     * Read client to check whether it is using the default Token Endpoint Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "omittedTokenEndpointAuthMethodStep1")
    public void omittedTokenEndpointAuthMethodStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken1);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri1.substring(registrationClientUri1.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("omittedTokenEndpointAuthMethodStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void tokenEndpointAuthMethodClientSecretBasicStep1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
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
                showResponse("tokenEndpointAuthMethodClientSecretBasicStep1", response);

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
     * Read client to check whether it is using the Token Endpoint Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep1")
    public void tokenEndpointAuthMethodClientSecretBasicStep2(final String registerPath) throws Exception {

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
                showResponse("tokenEndpointAuthMethodClientSecretBasicStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Request authorization code.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep2")
    public void tokenEndpointAuthMethodClientSecretBasicStep3(final String authorizePath,
                                                              final String userId, final String userSecret,
                                                              final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.CODE);
                List<String> scopes = new ArrayList<String>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("address");
                scopes.add("email");
                String state = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId2, scopes, redirectUri, null);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretBasicStep3", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Call to Token Endpoint with Auth Method <code>client_secret_basic</code>.
     */
    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"tokenEndpointAuthMethodClientSecretBasicStep3"})
    public void tokenEndpointAuthMethodClientSecretBasicStep4(final String tokenPath, final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
                tokenRequest.setCode(authorizationCode2);
                tokenRequest.setRedirectUri(redirectUri);
                tokenRequest.setAuthUsername(clientId2);
                tokenRequest.setAuthPassword(clientSecret2);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretBasicStep4", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
                    assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_post</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep2")
    public void tokenEndpointAuthMethodClientSecretBasicFail1(final String tokenPath,
                                                              final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId2);
                tokenRequest.setAuthPassword(clientSecret2);

                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretBasicFail1", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Fail 2: Call to Token Endpoint with Auth Method <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "audience", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep2")
    public void tokenEndpointAuthMethodClientSecretBasicFail2(final String tokenPath, final String audience,
                                                              final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
                tokenRequest.setAudience(audience);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId2);
                tokenRequest.setAuthPassword(clientSecret2);

                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretBasicFail2", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Fail 3: Call to Token Endpoint with Auth Method <code>private_key_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret", "audience", "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretBasicStep2")
    public void tokenEndpointAuthMethodClientSecretBasicFail3(
            final String tokenPath, final String userId, final String userSecret, final String audience,
            final String keyId, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

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

                    request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                    request.addParameters(tokenRequest.getParameters());
                } catch (Exception ex) {
                    fail(ex.getMessage(), ex);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretBasicFail3", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>client_secret_post</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void tokenEndpointAuthMethodClientSecretPostStep1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
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
                showResponse("tokenEndpointAuthMethodClientSecretPostStep1", response);

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
                    registrationClientUri3 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Token Endpoint Auth Method <code>client_secret_post</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep1")
    public void tokenEndpointAuthMethodClientSecretPostStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken3);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri3.substring(registrationClientUri3.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretPostStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Request authorization code.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep2")
    public void tokenEndpointAuthMethodClientSecretPostStep3(final String authorizePath,
                                                             final String userId, final String userSecret,
                                                             final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.CODE);
                List<String> scopes = new ArrayList<String>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("address");
                scopes.add("email");
                String state = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId3, scopes, redirectUri, null);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretPostStep3", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Call to Token Endpoint with Auth Method <code>client_secret_post</code>.
     */
    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"tokenEndpointAuthMethodClientSecretPostStep3"})
    public void tokenEndpointAuthMethodClientSecretPostStep4(final String tokenPath, final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
                tokenRequest.setCode(authorizationCode3);
                tokenRequest.setRedirectUri(redirectUri);
                tokenRequest.setAuthUsername(clientId3);
                tokenRequest.setAuthPassword(clientSecret3);

                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretBasicStep4", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
                    assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep2")
    public void tokenEndpointAuthMethodClientSecretPostFail1(final String tokenPath,
                                                             final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId3);
                tokenRequest.setAuthPassword(clientSecret3);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretPostFail1", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Fail 2: Call to Token Endpoint with Auth Method <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "audience", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep2")
    public void tokenEndpointAuthMethodClientSecretPostFail2(final String tokenPath, final String audience,
                                                             final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
                tokenRequest.setAudience(audience);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId3);
                tokenRequest.setAuthPassword(clientSecret3);

                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretPostFail2", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Fail 3: Call to Token Endpoint with Auth Method <code>private_key_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret", "audience", "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretPostStep2")
    public void tokenEndpointAuthMethodClientSecretPostFail3(
            final String tokenPath, final String userId, final String userSecret, final String audience,
            final String keyId, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

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

                    request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                    request.addParameters(tokenRequest.getParameters());
                } catch (Exception ex) {
                    fail(ex.getMessage(), ex);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretPostFail3", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>client_secret_jwt</code>.
     */
    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void tokenEndpointAuthMethodClientSecretJwtStep1(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
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
                showResponse("tokenEndpointAuthMethodClientSecretJwtStep1", response);

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
                    registrationClientUri4 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Token Endpoint Auth Method <code>client_secret_jwt</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep1")
    public void tokenEndpointAuthMethodClientSecretJwtStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken4);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri4.substring(registrationClientUri4.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretJwtStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Request authorization code.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep2")
    public void tokenEndpointAuthMethodClientSecretJwtStep3(final String authorizePath,
                                                            final String userId, final String userSecret,
                                                            final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.CODE);
                List<String> scopes = new ArrayList<String>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("address");
                scopes.add("email");
                String state = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId4, scopes, redirectUri, null);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretJwtStep3", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Call to Token Endpoint with Auth Method <code>client_secret_Jwt</code>.
     */
    @Parameters({"tokenPath", "redirectUri", "audience", "RS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = {"tokenEndpointAuthMethodClientSecretJwtStep3"})
    public void tokenEndpointAuthMethodClientSecretJwtStep4(
            final String tokenPath, final String redirectUri, final String audience, final String keyId,
            final String dnName, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

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

                    request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                    request.addParameters(tokenRequest.getParameters());
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretJwtStep4", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
                    assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");
                } catch (Exception e) {
                    fail(e.getMessage(), e);
                }
            }
        }.run();
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep2")
    public void tokenEndpointAuthMethodClientSecretJwtFail1(final String tokenPath,
                                                            final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId4);
                tokenRequest.setAuthPassword(clientSecret4);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretJwtFail1", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Fail 2: Call to Token Endpoint with Auth Method <code>client_secret_post</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep2")
    public void tokenEndpointAuthMethodClientSecretJwtFail2(final String tokenPath,
                                                            final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId4);
                tokenRequest.setAuthPassword(clientSecret4);

                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretJwtFail2", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Fail 3: Call to Token Endpoint with Auth Method <code>private_key_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret", "audience", "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodClientSecretJwtStep2")
    public void tokenEndpointAuthMethodClientSecretJwtFail3(
            final String tokenPath, final String userId, final String userSecret, final String audience,
            final String keyId, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

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

                    request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                    request.addParameters(tokenRequest.getParameters());
                } catch (Exception ex) {
                    fail(ex.getMessage(), ex);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodClientSecretJwtFail3", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Register a client with Token Endpoint Auth Method <code>private_key_jwt</code>.
     */
    @Parameters({"registerPath", "redirectUris", "clientJwksUri"})
    @Test
    public void tokenEndpointAuthMethodPrivateKeyJwtStep1(final String registerPath, final String redirectUris,
                                                          final String jwksUri) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
                    registerRequest.setJwksUri(jwksUri);
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
                showResponse("tokenEndpointAuthMethodPrivateKeyJwtStep1", response);

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
                    registrationClientUri5 = jsonObj.getString(RegisterResponseParam.REGISTRATION_CLIENT_URI.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Read client to check whether it is using the Token Endpoint Auth Method <code>private_key_jwt</code>.
     */
    @Parameters({"registerPath"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep1")
    public void tokenEndpointAuthMethodPrivateKeyJwtStep2(final String registerPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Authorization", "Bearer " + registrationAccessToken5);
                request.setContentType(MediaType.APPLICATION_JSON);
                request.setQueryString(registrationClientUri5.substring(registrationClientUri5.indexOf("?") + 1));
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodPrivateKeyJwtStep2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
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
                    assertTrue(jsonObj.has("scopes"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Request authorization code.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep2")
    public void tokenEndpointAuthMethodPrivateKeyJwtStep3(final String authorizePath,
                                                          final String userId, final String userSecret,
                                                          final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.CODE);
                List<String> scopes = new ArrayList<String>();
                scopes.add("openid");
                scopes.add("profile");
                scopes.add("address");
                scopes.add("email");
                String state = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId5, scopes, redirectUri, null);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodPrivateKeyJwtStep3", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    /**
     * Call to Token Endpoint with Auth Method <code>private_key_jwt</code>.
     */
    @Parameters({"tokenPath", "redirectUri", "audience", "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
    @Test(dependsOnMethods = {"tokenEndpointAuthMethodPrivateKeyJwtStep3"})
    public void tokenEndpointAuthMethodPrivateKeyJwtStep4(
            final String tokenPath, final String redirectUri, final String audience,
            final String keyId, final String keyStoreFile, final String keyStoreSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

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

                    request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                    request.addParameters(tokenRequest.getParameters());
                } catch (Exception ex) {
                    fail(ex.getMessage(), ex);
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodPrivateKeyJwtStep4", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
                    assertTrue(jsonObj.has("id_token"), "Unexpected result: id_token not found");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }

    /**
     * Fail 1: Call to Token Endpoint with Auth Method <code>client_secret_basic</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep2")
    public void tokenEndpointAuthMethodPrivateKeyJwtFail1(final String tokenPath,
                                                          final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId5);
                tokenRequest.setAuthPassword(clientSecret5);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodPrivateKeyJwtFail1", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Fail 2: Call to Token Endpoint with Auth Method <code>client_secret_post</code> should fail.
     */
    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep2")
    public void tokenEndpointAuthMethodPrivateKeyJwtFail2(final String tokenPath,
                                                          final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId5);
                tokenRequest.setAuthPassword(clientSecret5);

                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodPrivateKeyJwtFail2", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Fail 3: Call to Token Endpoint with Auth Method <code>client_secret_jwt</code> should fail.
     */
    @Parameters({"tokenPath", "audience", "userId", "userSecret"})
    @Test(dependsOnMethods = "tokenEndpointAuthMethodPrivateKeyJwtStep2")
    public void tokenEndpointAuthMethodPrivateKeyJwtFail3(final String tokenPath, final String audience,
                                                          final String userId, final String userSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
                tokenRequest.setAudience(audience);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId5);
                tokenRequest.setAuthPassword(clientSecret5);

                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenEndpointAuthMethodPrivateKeyJwtFail3", response);

                assertEquals(response.getStatus(), 401, "Unexpected response code.");
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("error"), "The error type is null");
                    assertTrue(jsonObj.has("error_description"), "The error description is null");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }
}
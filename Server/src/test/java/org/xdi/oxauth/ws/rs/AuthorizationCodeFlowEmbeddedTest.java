/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.mock.AbstractSeamTest;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.jboss.seam.mock.ResourceRequestEnvironment.Method;
import org.jboss.seam.mock.ResourceRequestEnvironment.ResourceRequest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.authorize.AuthorizeResponseParam;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.core.MediaType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * Test cases for the authorization code flow (embedded)
 *
 * @author Javier Rojas Blum
 * @version 0.9 March 5, 2015
 */
public class AuthorizationCodeFlowEmbeddedTest extends BaseTest {

    private String clientId;
    private String clientSecret;
    private String authorizationCode1;
    private String authorizationCode2;
    private String authorizationCode3;
    private String authorizationCode4;
    private String accessToken1;
    private String refreshToken1;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {

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
                showResponse("dynamicClientRegistration", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    final RegisterResponse registerResponse = RegisterResponse.valueOf(response.getContentAsString());
                    ClientTestUtil.assert_(registerResponse);

                    clientId = registerResponse.getClientId();
                    clientSecret = registerResponse.getClientSecret();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    /**
     * Test for the complete Authorization Code Flow:
     * 1. Request authorization and receive the authorization code.
     * 2. Request access token using the authorization code.
     * 3. Validate access token.
     * 4. Request new access token using the refresh token.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void completeFlowStep1(final String authorizePath,
                                  final String userId, final String userSecret,
                                  final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
                List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, null);
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
                showResponse("completeFlowStep1", response);

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
        }.run();
    }

    @Parameters({"tokenPath", "validateTokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "completeFlowStep1"})
    public void completeFlowStep2(final String tokenPath, final String validateTokenPath,
                                  final String redirectUri) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setCode(authorizationCode1);
                tokenRequest.setRedirectUri(redirectUri);
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("completeFlowStep2", response);

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

                    String accessToken = jsonObj.getString("access_token");
                    String refreshToken = jsonObj.getString("refresh_token");

                    completeFlowStep3(tokenPath, validateTokenPath, accessToken, refreshToken);
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

    public void completeFlowStep3(final String tokenPath, final String validateTokenPath,
                                  final String accessToken, final String refreshToken) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, validateTokenPath) {
            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                ValidateTokenRequest validateTokenRequest = new ValidateTokenRequest();
                validateTokenRequest.setAccessToken(accessToken);

                request.setQueryString(validateTokenRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("completeFlowStep3", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store, private"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());

                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("valid"), "Unexpected result: valid not found");
                    assertTrue(jsonObj.getBoolean("valid"), "Unexpected result: valid is false");
                    assertTrue(jsonObj.has("expires_in"), "Unexpected result: expires_in not found");

                    completeFlowStep4(tokenPath, refreshToken);
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

    public void completeFlowStep4(final String tokenPath, final String refreshToken) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.REFRESH_TOKEN);
                tokenRequest.setRefreshToken(refreshToken);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("completeFlowStep4", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(),
                        "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
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

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void completeFlowWithOptionalNonceStep1(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
                List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, nonce);
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
                showResponse("completeFlowWithOptionalNonceStep1", response);

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
                        assertEquals(params.get(AuthorizeResponseParam.STATE), state);

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

    @Parameters({"tokenPath", "validateTokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "completeFlowWithOptionalNonceStep1"})
    public void completeFlowWithOptionalNonceStep2(final String tokenPath, final String validateTokenPath,
                                                   final String redirectUri) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setCode(authorizationCode4);
                tokenRequest.setRedirectUri(redirectUri);
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("completeFlowWithOptionalNonceStep2", response);

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

                    String accessToken = jsonObj.getString("access_token");
                    String refreshToken = jsonObj.getString("refresh_token");
                    String idToken = jsonObj.getString("id_token");
                    Jwt jwt = Jwt.parse(idToken);
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.NONCE));

                    completeFlowWithOptionalNonceStep3(tokenPath, validateTokenPath, accessToken, refreshToken);
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

    public void completeFlowWithOptionalNonceStep3(final String tokenPath, final String validateTokenPath,
                                                   final String accessToken, final String refreshToken) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, validateTokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                ValidateTokenRequest validateTokenRequest = new ValidateTokenRequest();
                validateTokenRequest.setAccessToken(accessToken);

                request.setQueryString(validateTokenRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("completeFlowWithOptionalNonceStep3", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code. " + response.getContentAsString());
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store, private"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());

                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("valid"), "Unexpected result: valid not found");
                    assertTrue(jsonObj.getBoolean("valid"), "Unexpected result: valid is false");
                    assertTrue(jsonObj.has("expires_in"), "Unexpected result: expires_in not found");

                    completeFlowWithOptionalNonceStep4(tokenPath, refreshToken);
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

    public void completeFlowWithOptionalNonceStep4(final String tokenPath, final String refreshToken) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.REFRESH_TOKEN);
                tokenRequest.setRefreshToken(refreshToken);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("completeFlowWithOptionalNonceStep4", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(),
                        "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
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
     * When an authorization code is used more than once, all the tokens issued
     * for that authorization code must be revoked:
     * 1. Request authorization and receive the authorization code.
     * 2. Request access token using the authorization code.
     * 3. Request access token using the same authorization code one more time. This call must fail.
     * 4. Request new access token using the refresh token. This call must fail too.
     * 5. Request user info must fail.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void revokeTokensStep1(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
                List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, null);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);
                authorizationRequest.setState(state);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("revokeTokensStep1", response);

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
                        assertEquals(params.get(AuthorizeResponseParam.STATE), state);

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

    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "revokeTokensStep1"})
    public void revokeTokensStep2n3(final String tokenPath, final String redirectUri) throws Exception {
        final AbstractSeamTest test = this;

        new ResourceRequest(new ResourceRequestEnvironment(this), Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setCode(authorizationCode2);
                tokenRequest.setRedirectUri(redirectUri);
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("revokeTokensStep2n3", response);

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

                    accessToken1 = jsonObj.getString("access_token");
                    refreshToken1 = jsonObj.getString("refresh_token");

                    new ResourceRequest(new ResourceRequestEnvironment(test), Method.POST, tokenPath) {

                        @Override
                        protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                            super.prepareRequest(request);

                            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                            tokenRequest.setCode(authorizationCode2);
                            tokenRequest.setRedirectUri(redirectUri);
                            tokenRequest.setAuthUsername(clientId);
                            tokenRequest.setAuthPassword(clientSecret);

                            request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                            request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                            request.addParameters(tokenRequest.getParameters());
                        }

                        @Override
                        protected void onResponse(EnhancedMockHttpServletResponse response) {
                            super.onResponse(response);
                            showResponse("revokeTokens step 3", response);

                            assertEquals(response.getStatus(), 400, "Unexpected response code.");
                            assertNotNull(response.getContentAsString(),
                                    "Unexpected result: " + response.getContentAsString());
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

    @Parameters({"tokenPath"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "revokeTokensStep2n3"})
    public void revokeTokensStep4(final String tokenPath) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.REFRESH_TOKEN);
                tokenRequest.setRefreshToken(refreshToken1);
                tokenRequest.setScope("email read_stream manage_pages");
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("revokeTokensStep4", response);

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

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "revokeTokensStep4")
    public void revokeTokensStep5(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken1);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest(null);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("revokeTokensStep5", response);

                assertEquals(response.getStatus(), 400, "Unexpected response code.");
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
     * Test to verify the token expiration
     * 1. Request authorization and receive the authorization code.
     * ...Wait until the authorization code expires...
     * 2. Request access token using the expired authorization code. This call
     * must fail.
     *
     * @throws Exception
     */
    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void tokenExpirationStep1(final String authorizePath,
                                     final String userId, final String userSecret,
                                     final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
                List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, null);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);
                authorizationRequest.setState(state);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenExpirationStep1", response);

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
                        assertEquals(params.get(AuthorizeResponseParam.STATE), state);

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

    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = {"dynamicClientRegistration", "tokenExpirationStep1"})
    public void tokenExpirationStep2(final String tokenPath, final String redirectUri) throws Exception {
        // ...Wait until the authorization code expires...
        System.out.println("Sleeping for 20 seconds .....");
        Thread.sleep(20000);

        new ResourceRequest(new ResourceRequestEnvironment(this), Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
                tokenRequest.setCode(authorizationCode3);
                tokenRequest.setRedirectUri(redirectUri);
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("tokenExpirationStep2", response);

                assertEquals(response.getStatus(), 400, "Unexpected response code.");
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
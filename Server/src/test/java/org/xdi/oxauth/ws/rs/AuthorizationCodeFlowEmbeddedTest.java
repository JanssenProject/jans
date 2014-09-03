package org.xdi.oxauth.ws.rs;

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

import javax.ws.rs.core.MediaType;

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
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.UserInfoRequest;
import org.xdi.oxauth.client.ValidateTokenRequest;
import org.xdi.oxauth.model.authorize.AuthorizeResponseParam;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;

/**
 * Test cases for the authorization code flow (embedded)
 *
 * @author Javier Rojas Blum
 * @version 0.9, 08/14/2014
 */
public class AuthorizationCodeFlowEmbeddedTest extends BaseTest {

    String authorizationCode1;
    String authorizationCode2;
    String authorizationCode3;
    String authorizationCode4;
    String accessToken1;
    String refreshToken1;

    /**
     * Test for the complete Authorization Code Flow:
     * 1. Request authorization and receive the authorization code.
     * 2. Request access token using the authorization code.
     * 3. Validate access token.
     * 4. Request new access token using the refresh token.
     */
    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void completeFlowStep1(final String authorizePath,
                                  final String userId, final String userSecret,
                                  final String clientId, final String redirectUri) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, authorizePath) {

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
                String state = "af0ifjsldkj";

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

    @Parameters({"tokenPath", "validateTokenPath", "clientId", "clientSecret", "redirectUri"})
    @Test(dependsOnMethods = {"completeFlowStep1"})
    public void completeFlowStep2(final String tokenPath, final String validateTokenPath,
                                  final String clientId, final String clientSecret,
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
                    String idToken = jsonObj.getString("id_token");

                    completeFlowStep3(tokenPath, validateTokenPath, accessToken, refreshToken, clientId, clientSecret);
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
                                  final String accessToken, final String refreshToken,
                                  final String clientId, final String clientSecret) throws Exception {
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

                    completeFlowStep4(tokenPath, refreshToken, clientId, clientSecret);
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

    public void completeFlowStep4(final String tokenPath, final String refreshToken,
                                  final String clientId, final String clientSecret) throws Exception {
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

    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void completeFlowWithOptionalNonceStep1(final String authorizePath,
                                                   final String userId, final String userSecret,
                                                   final String clientId, final String redirectUri) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, authorizePath) {

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
                String state = "af0ifjsldkj";
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

    @Parameters({"tokenPath", "validateTokenPath", "clientId", "clientSecret", "redirectUri"})
    @Test(dependsOnMethods = {"completeFlowWithOptionalNonceStep1"})
    public void completeFlowWithOptionalNonceStep2(final String tokenPath, final String validateTokenPath,
                                                   final String clientId, final String clientSecret,
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

                    completeFlowWithOptionalNonceStep3(tokenPath, validateTokenPath, accessToken, refreshToken, clientId, clientSecret);
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
                                                   final String accessToken, final String refreshToken,
                                                   final String clientId, final String clientSecret) throws Exception {
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

                    completeFlowWithOptionalNonceStep4(tokenPath, refreshToken, clientId, clientSecret);
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

    public void completeFlowWithOptionalNonceStep4(final String tokenPath, final String refreshToken,
                                                   final String clientId, final String clientSecret) throws Exception {
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
    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void revokeTokensStep1(final String authorizePath,
                                  final String userId, final String userSecret,
                                  final String clientId, final String redirectUri) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, authorizePath) {

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

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, null);
                authorizationRequest.setState("af0ifjsldkj");
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

    @Parameters({"tokenPath", "clientId", "clientSecret", "redirectUri"})
    @Test(dependsOnMethods = {"revokeTokensStep1"})
    public void revokeTokensStep2n3(final String tokenPath, final String clientId, final String clientSecret,
                                    final String redirectUri) throws Exception {
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

    @Parameters({"tokenPath", "clientId", "clientSecret"})
    @Test(dependsOnMethods = {"revokeTokensStep2n3"})
    public void revokeTokensStep4(final String tokenPath, final String clientId, final String clientSecret)
            throws Exception {
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
    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void tokenExpirationStep1(final String authorizePath,
                                     final String userId, final String userSecret, final String clientId,
                                     final String redirectUri) throws Exception {
        new ResourceRequest(new ResourceRequestEnvironment(this), Method.GET, authorizePath) {

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

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, null);
                authorizationRequest.setState("af0ifjsldkj");
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

    @Parameters({"tokenPath", "clientId", "clientSecret", "redirectUri"})
    @Test(dependsOnMethods = {"tokenExpirationStep1"})
    public void tokenExpirationStep2(final String tokenPath, final String clientId, final String clientSecret,
                                     final String redirectUri) throws Exception {
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
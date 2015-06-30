/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.jboss.seam.mock.ResourceRequestEnvironment.Method;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.client.model.authorize.Claim;
import org.xdi.oxauth.client.model.authorize.ClaimValue;
import org.xdi.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.authorize.AuthorizeResponseParam;
import org.xdi.oxauth.model.common.AuthorizationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.core.MediaType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;

/**
 * Functional tests for User Info Web Services (embedded)
 *
 * @author Javier Rojas Blum
 * @version 0.9 April 27, 2015
 */
public class UserInfoRestWebServiceEmbeddedTest extends BaseTest {

    private String clientId;
    private String clientSecret;

    private String accessToken1;
    private String accessToken2;
    private String accessToken3;
    private String accessToken4;
    private String accessToken5;
    private String accessToken6;
    private String accessToken7;
    private String clientId1;
    private String clientId2;
    private String clientId3;
    private String clientSecret1;
    private String clientSecret2;
    private String clientSecret3;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, registerPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                try {
                    super.prepareRequest(request);

                    List<ResponseType> responseTypes = Arrays.asList(
                            ResponseType.CODE,
                            ResponseType.TOKEN,
                            ResponseType.ID_TOKEN);

                    RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                            StringUtils.spaceSeparatedToList(redirectUris));
                    registerRequest.setResponseTypes(responseTypes);

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

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestUserInfoStep1ImplicitFlow(final String authorizePath,
                                                 final String userId, final String userSecret,
                                                 final String redirectUri) throws Exception {
        final String userEncodedCredentials = Base64.encodeBase64String((userId + ":" + userSecret).getBytes());
        final String state = UUID.randomUUID().toString();

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Basic " + userEncodedCredentials);
                request.addHeader("Accept", MediaType.TEXT_PLAIN);

                List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                List<String> scopes = Arrays.asList(
                        "openid",
                        "profile",
                        "address",
                        "email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, nonce);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);

                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfo step 1 Implicit Flow", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoStep1ImplicitFlow")
    public void requestUserInfoStep2PostImplicitFlow(final String userInfoPath) throws Exception {
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
                showResponse("requestUserInfo step 2 POST Implicit Flow", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(JwtClaimName.SUBJECT_IDENTIFIER));
                    assertTrue(jsonObj.has(JwtClaimName.NAME));
                    assertTrue(jsonObj.has(JwtClaimName.GIVEN_NAME));
                    assertTrue(jsonObj.has(JwtClaimName.FAMILY_NAME));
                    assertTrue(jsonObj.has(JwtClaimName.EMAIL));
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

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoStep1ImplicitFlow")
    public void requestUserInfoStep2GetImplicitFlow(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.GET, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken1);

                UserInfoRequest userInfoRequest = new UserInfoRequest(null);

                request.setQueryString(userInfoRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfo step 2 GET Implicit Flow", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(JwtClaimName.SUBJECT_IDENTIFIER));
                    assertTrue(jsonObj.has(JwtClaimName.NAME));
                    assertTrue(jsonObj.has(JwtClaimName.GIVEN_NAME));
                    assertTrue(jsonObj.has(JwtClaimName.FAMILY_NAME));
                    assertTrue(jsonObj.has(JwtClaimName.EMAIL));
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

    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestUserInfoStep1PasswordFlow(final String tokenPath, final String userId, final String userSecret)
            throws Exception {
        // Testing with valid parameters
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("openid profile address email");
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoStep1PasswordFlow", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null
                                && response.getHeader("Cache-Control").equals("no-store"),
                        "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null
                                && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertTrue(!response.getContentAsString().equals(null), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
                    assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
                    assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
                    assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");

                    accessToken4 = jsonObj.getString("access_token");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoStep1PasswordFlow")
    public void requestUserInfoStep2PasswordFlow(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken4);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest(null);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoStep2PasswordFlow", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(JwtClaimName.SUBJECT_IDENTIFIER));
                    assertTrue(jsonObj.has(JwtClaimName.NAME));
                    assertTrue(jsonObj.has(JwtClaimName.GIVEN_NAME));
                    assertTrue(jsonObj.has(JwtClaimName.FAMILY_NAME));
                    assertTrue(jsonObj.has(JwtClaimName.EMAIL));
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

    @Parameters({"userInfoPath"})
    @Test
    public void requestUserInfoInvalidRequest(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest(null);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoInvalidRequest", response);

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

    @Parameters({"userInfoPath"})
    @Test
    public void requestUserInfoInvalidToken(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest("INVALID_ACCESS_TOKEN");
                userInfoRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoInvalidToken", response);

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

    @Parameters({"userInfoPath"})
    @Test
    public void requestUserInfoInvalidSchema(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest("INVALID_ACCESS_TOKEN");

                Map<String, String> userInfoParameters = userInfoRequest.getParameters();
                userInfoParameters.put("schema", "INVALID_SCHEMA");
                request.addParameters(userInfoParameters);
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoInvalidSchema", response);

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

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestUserInfoInsufficientScope(final String authorizePath, final String userId, final String userSecret,
                                                 final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.TOKEN);
                List<String> scopes = Arrays.asList("picture");
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
                showResponse("requestUserInfoInsufficientScope step 1", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
                        assertNotNull(uri.getFragment(), "Fragment is null");

                        Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                        assertNotNull(params.get(AuthorizeResponseParam.ACCESS_TOKEN), "The access token is null");
                        assertNotNull(params.get(AuthorizeResponseParam.TOKEN_TYPE), "The token type is null");
                        assertNotNull(params.get(AuthorizeResponseParam.EXPIRES_IN), "The expires in value is null");
                        assertNotNull(params.get(AuthorizeResponseParam.SCOPE), "The scope must be null");
                        assertNull(params.get("refresh_token"), "The refresh_token must be null");
                        assertNotNull(params.get(AuthorizeResponseParam.STATE), "The state is null");
                        assertEquals(params.get(AuthorizeResponseParam.STATE), state);

                        accessToken2 = params.get(AuthorizeResponseParam.ACCESS_TOKEN);
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

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoInsufficientScope")
    public void requestUserInfoInsufficientScopeStep2(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken2);
                userInfoRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoInsufficientScope step 2", response);

                assertEquals(response.getStatus(), 403, "Unexpected response code.");
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

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestUserInfoAdditionalClaims(final String authorizePath, final String userId, final String userSecret,
                                                final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.TOKEN);
                List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, nonce);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret);
                jwtAuthorizationRequest.addUserInfoClaim(new Claim("invalid", ClaimValue.createEssential(false)));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim("iname", ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim("o", ClaimValue.createEssential(true)));

                String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                authorizationRequest.setRequest(authJwt);
                System.out.println("Request JWT: " + authJwt);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoAdditionalClaims step 1", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoAdditionalClaims")
    public void requestUserInfoAdditionalClaimsStep2(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken3);
                userInfoRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoAdditionalClaims step 2", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(JwtClaimName.SUBJECT_IDENTIFIER));
                    assertTrue(jsonObj.has(JwtClaimName.NAME));
                    assertTrue(jsonObj.has(JwtClaimName.GIVEN_NAME));
                    assertTrue(jsonObj.has(JwtClaimName.FAMILY_NAME));
                    assertTrue(jsonObj.has(JwtClaimName.EMAIL));

                    // Custom attributes
                    assertTrue(jsonObj.has("iname"));
                    assertTrue(jsonObj.has("o"));
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

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestUserInfoHS256Step1(final String registerPath, final String redirectUris) throws Exception {

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
                    registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS256);
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
                showResponse("requestUserInfoHS256Step1", response);

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
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestUserInfoHS256Step1")
    public void requestUserInfoHS256Step2(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                List<String> scopes = Arrays.asList("openid");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId1, scopes, redirectUri, nonce);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret1);
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                authorizationRequest.setRequest(authJwt);
                System.out.println("Request JWT: " + authJwt);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoHS256Step2", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoHS256Step2")
    public void requestUserInfoHS256Step3(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken5);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest(null);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoHS256Step3", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());

                try {
                    Jwt jwt = Jwt.parse(response.getContentAsString());

                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.NAME));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EMAIL));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.PICTURE));
                } catch (InvalidJwtException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestUserInfoHS384Step1(final String registerPath, final String redirectUris) throws Exception {

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
                    registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS384);
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
                showResponse("requestUserInfoHS384Step1", response);

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
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestUserInfoHS384Step1")
    public void requestUserInfoHS384Step2(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                List<String> scopes = Arrays.asList("openid");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId2, scopes, redirectUri, nonce);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS384, clientSecret2);
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                authorizationRequest.setRequest(authJwt);
                System.out.println("Request JWT: " + authJwt);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoHS384Step2", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoHS384Step2")
    public void requestUserInfoHS384Step3(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken6);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest(null);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoHS384Step3", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());

                try {
                    Jwt jwt = Jwt.parse(response.getContentAsString());

                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.NAME));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EMAIL));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.PICTURE));
                } catch (InvalidJwtException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void requestUserInfoHS512Step1(final String registerPath, final String redirectUris) throws Exception {

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
                    registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS512);
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
                showResponse("requestUserInfoHS512Step1", response);

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
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestUserInfoHS512Step1")
    public void requestUserInfoHS512Step2(
            final String authorizePath, final String userId, final String userSecret, final String redirectUri) throws Exception {

        final String state = UUID.randomUUID().toString();
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
                List<String> scopes = Arrays.asList("openid");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId3, scopes, redirectUri, nonce);
                authorizationRequest.setState(state);
                authorizationRequest.getPrompts().add(Prompt.NONE);
                authorizationRequest.setAuthUsername(userId);
                authorizationRequest.setAuthPassword(userSecret);

                JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS512, clientSecret3);
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
                jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
                String authJwt = jwtAuthorizationRequest.getEncodedJwt();
                authorizationRequest.setRequest(authJwt);
                System.out.println("Request JWT: " + authJwt);

                request.addHeader("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
                request.addHeader("Accept", MediaType.TEXT_PLAIN);
                request.setQueryString(authorizationRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoHS512Step2", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                try {
                    URI uri = new URI(response.getHeader("Location").toString());
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
        }.run();
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestUserInfoHS512Step2")
    public void requestUserInfoHS512Step3(final String userInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                Method.POST, userInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken7);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                UserInfoRequest userInfoRequest = new UserInfoRequest(null);

                request.addParameters(userInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestUserInfoHS512Step3", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());

                try {
                    Jwt jwt = Jwt.parse(response.getContentAsString());

                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.NAME));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EMAIL));
                    assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.PICTURE));
                } catch (InvalidJwtException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }
}
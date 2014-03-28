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
import org.xdi.oxauth.client.ClientInfoRequest;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.model.common.AuthorizationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * Functional tests for Client Info Web Services (embedded)
 *
 * @author Javier Rojas Blum Date: 07.19.2012
 */
public class ClientInfoRestWebServiceEmbeddedTest extends BaseTest {

    private String accessToken1;
    private String accessToken2;
    private String accessToken3;

    @Parameters({"authorizePath", "userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestClientInfoStep1ImplicitFlow(final String authorizePath,
                                                   final String userId, final String userSecret,
                                                   final String clientId, final String redirectUri) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, authorizePath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                List<ResponseType> responseTypes = new ArrayList<ResponseType>();
                responseTypes.add(ResponseType.TOKEN);
                List<String> scopes = new ArrayList<String>();
                scopes.add("clientinfo");
                String nonce = UUID.randomUUID().toString();

                AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                        responseTypes, clientId, scopes, redirectUri, nonce);
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
                showResponse("requestClientInfo step 1 Implicit Flow", response);

                assertEquals(response.getStatus(), 302, "Unexpected response code.");
                assertNotNull(response.getHeader("Location"), "Unexpected result: " + response.getHeader("Location"));

                if (response.getHeader("Location") != null) {
                    try {
                        URI uri = new URI(response.getHeader("Location").toString());
                        assertNotNull(uri.getFragment(), "Fragment is null");

                        Map<String, String> params = QueryStringDecoder.decode(uri.getFragment());

                        assertNotNull(params.get("access_token"), "The access token is null");
                        assertNotNull(params.get("state"), "The state is null");
                        assertNotNull(params.get("token_type"), "The token type is null");
                        assertNotNull(params.get("expires_in"), "The expires in value is null");
                        assertNotNull(params.get("scope"), "The scope must be null");
                        assertNull(params.get("refresh_token"), "The refresh_token must be null");

                        accessToken1 = params.get("access_token");
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

    @Parameters({"clientInfoPath"})
    @Test(dependsOnMethods = "requestClientInfoStep1ImplicitFlow")
    public void requestClientInfoStep2PostImplicitFlow(final String clientInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, clientInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken1);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                ClientInfoRequest clientInfoRequest = new ClientInfoRequest(null);

                request.addParameters(clientInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestClientInfo step 2 POST Implicit Flow", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("displayName"), "Unexpected result: displayName not found");
                    assertTrue(jsonObj.has("inum"), "Unexpected result: inum not found");
                    assertTrue(jsonObj.has("oxAuthAppType"), "Unexpected result: oxAuthAppType not found");
                    assertTrue(jsonObj.has("oxAuthIdTokenSignedResponseAlg"), "Unexpected result: oxAuthIdTokenSignedResponseAlg not found");
                    assertTrue(jsonObj.has("oxAuthRedirectURI"), "Unexpected result: oxAuthRedirectURI not found");
                    assertTrue(jsonObj.has("oxAuthScope"), "Unexpected result: oxAuthScope not found");
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

    @Parameters({"clientInfoPath"})
    @Test(dependsOnMethods = "requestClientInfoStep1ImplicitFlow")
    public void requestClientInfoStep2GetImplicitFlow(final String clientInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, clientInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken1);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                ClientInfoRequest clientInfoRequest = new ClientInfoRequest(null);

                request.setQueryString(clientInfoRequest.getQueryString());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestClientInfo step 2 GET Implicit Flow", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                assertTrue(response.getHeader("Cache-Control") != null && response.getHeader("Cache-Control").equals(
                        "no-store, private"), "Unexpected result: " + response.getHeader("Cache-Control"));
                assertTrue(response.getHeader("Pragma") != null && response.getHeader("Pragma").equals("no-cache"),
                        "Unexpected result: " + response.getHeader("Pragma"));
                assertNotNull(response.getContentAsString(), "Unexpected result: " + response.getContentAsString());
                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has("displayName"), "Unexpected result: displayName not found");
                    assertTrue(jsonObj.has("inum"), "Unexpected result: inum not found");
                    assertTrue(jsonObj.has("oxAuthAppType"), "Unexpected result: oxAuthAppType not found");
                    assertTrue(jsonObj.has("oxAuthIdTokenSignedResponseAlg"), "Unexpected result: oxAuthIdTokenSignedResponseAlg not found");
                    assertTrue(jsonObj.has("oxAuthRedirectURI"), "Unexpected result: oxAuthRedirectURI not found");
                    assertTrue(jsonObj.has("oxAuthScope"), "Unexpected result: oxAuthScope not found");
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

    @Parameters({"tokenPath", "userId", "userSecret", "clientId", "clientSecret"})
    @Test
    public void requestClientInfoStep1PasswordFlow(final String tokenPath, final String userId, final String userSecret,
                                                   final String clientId, final String clientSecret) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, tokenPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
                tokenRequest.setUsername(userId);
                tokenRequest.setPassword(userSecret);
                tokenRequest.setScope("clientinfo");
                tokenRequest.setAuthUsername(clientId);
                tokenRequest.setAuthPassword(clientSecret);

                request.addHeader("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
                request.addParameters(tokenRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestClientInfoStep1PasswordFlow", response);

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

                    accessToken3 = jsonObj.getString("access_token");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage() + "\nResponse was: " + response.getContentAsString());
                }
            }
        }.run();
    }

    @Parameters({"clientInfoPath"})
    @Test(dependsOnMethods = "requestClientInfoStep1PasswordFlow")
    public void requestClientInfoStep2PasswordFlow(final String clientInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, clientInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Authorization", "Bearer " + accessToken3);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                ClientInfoRequest clientInfoRequest = new ClientInfoRequest(null);

                request.addParameters(clientInfoRequest.getParameters());
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
                    assertTrue(jsonObj.has("displayName"), "Unexpected result: displayName not found");
                    assertTrue(jsonObj.has("inum"), "Unexpected result: inum not found");
                    assertTrue(jsonObj.has("oxAuthAppType"), "Unexpected result: oxAuthAppType not found");
                    assertTrue(jsonObj.has("oxAuthIdTokenSignedResponseAlg"), "Unexpected result: oxAuthIdTokenSignedResponseAlg not found");
                    assertTrue(jsonObj.has("oxAuthRedirectURI"), "Unexpected result: oxAuthRedirectURI not found");
                    assertTrue(jsonObj.has("oxAuthScope"), "Unexpected result: oxAuthScope not found");
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

    @Parameters({"clientInfoPath"})
    @Test
    public void requestClientInfoInvalidRequest(final String clientInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, clientInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                ClientInfoRequest clientInfoRequest = new ClientInfoRequest(null);

                request.addParameters(clientInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestClientInfoInvalidRequest", response);

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

    @Parameters({"clientInfoPath"})
    @Test
    public void requestClientInfoInvalidToken(final String clientInfoPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.POST, clientInfoPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

                ClientInfoRequest clientInfoRequest = new ClientInfoRequest("INVALID-TOKEN");
                clientInfoRequest.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);

                request.addParameters(clientInfoRequest.getParameters());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestClientInfoInvalidToken", response);

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
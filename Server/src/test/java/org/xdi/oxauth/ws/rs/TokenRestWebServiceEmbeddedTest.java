/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.authorize.AuthorizeResponseParam;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterResponseParam;
import org.xdi.oxauth.model.util.StringUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.testng.Assert.*;
import static org.xdi.oxauth.model.register.RegisterResponseParam.*;

/**
 * Functional tests for Token Web Services (embedded)
 *
 * @author Javier Rojas Blum
 * @version April 26, 2017
 */
public class TokenRestWebServiceEmbeddedTest extends BaseTest {

    @ArquillianResource
    private URI url;

    private static String clientId;
    private static String clientSecret;
    private static String accessToken1;
    private static String clientId2;
    private static String clientSecret2;
    private static String accessToken2;

    @Parameters({"registerPath", "redirectUris"})
    @Test
    public void dynamicClientRegistration(final String registerPath, final String redirectUris) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.TOKEN,
                    ResponseType.ID_TOKEN);

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

            registerRequestContent = registerRequest.getJSONParameters().toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("dynamicClientRegistration", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            final RegisterResponse registerResponse = RegisterResponse.valueOf(entity);
            ClientTestUtil.assert_(registerResponse);

            clientId = registerResponse.getClientId();
            clientSecret = registerResponse.getClientSecret();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"tokenPath", "redirectUri"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestAccessToken(final String tokenPath, final String redirectUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode("6f6f3f01-a034-4336-bf31-2e74868e5838");
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessToken", response, entity);

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

    @Parameters({"tokenPath", "userId", "userSecret"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestAccessTokenPassword(final String tokenPath, final String userId, final String userSecret)
            throws Exception {
        // Testing with valid parameters
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenPassword", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("refresh_token"), "Unexpected result: refresh_token not found");
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"tokenPath", "userId", "userSecret", "audience"})
    @Test
    public void requestAccessTokenWithClientSecretJwtFail(final String tokenPath, final String userId,
                                                          final String userSecret, final String audience) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        TokenRequest tokenRequest = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        tokenRequest.setUsername(userId);
        tokenRequest.setPassword(userSecret);
        tokenRequest.setScope("email read_stream manage_pages");

        tokenRequest.setAuthPassword("INVALID_SECRET");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_JWT);
        tokenRequest.setAudience(audience);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenWithClientSecretJwt Fail", response, entity);

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

    @Parameters({"tokenPath"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestAccessTokenClientCredentials(final String tokenPath) throws Exception {
        // Testing with valid parameters
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenClientCredentials", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("scope"), "Unexpected result: scope not found");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"tokenPath"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void requestAccessTokenExtensions(final String tokenPath) throws Exception {
        // Testing with valid parameters
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        GrantType extension = GrantType.fromString("http://oauth.net/grant_type/assertion/saml/2.0/bearer");
        TokenRequest tokenRequest = new TokenRequest(extension);
        tokenRequest.setAssertion("PEFzc2VydGlvbiBJc3N1ZUluc3RhbnQV0aG5TdGF0ZW1lbnQPC9Bc3NlcnRpb24");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestAccessTokenExtensions", response, entity);

        assertEquals(response.getStatus(), 501, "Unexpected response code.");
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

    @Parameters({"tokenPath"})
    @Test(dependsOnMethods = "dynamicClientRegistration")
    public void refreshingAccessTokenFail(final String tokenPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();

        TokenRequest tokenRequest = new TokenRequest(GrantType.REFRESH_TOKEN);
        tokenRequest.setRefreshToken("tGzv3JOkF0XG5Qx2TlKWIA");
        tokenRequest.setScope("email read_stream manage_pages");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        request.header("Authorization", "Basic " + tokenRequest.getEncodedCredentials());
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("refreshingAccessTokenFail", response, entity);

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

    @Parameters({"registerPath", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestLongLivedAccessTokenStep1(final String registerPath, final String redirectUris,
                                                 final String sectorIdentifierUri) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + registerPath).request();

        String registerRequestContent = null;
        try {
            List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setTokenEndpointAuthMethod(AuthenticationMethod.CLIENT_SECRET_POST);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
            registerRequest.setSubjectType(SubjectType.PAIRWISE);
            registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

            registerRequestContent = registerRequest.getJSONParameters().toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Response response = request.post(Entity.json(registerRequestContent));
        String entity = response.readEntity(String.class);

        showResponse("requestLongLivedAccessTokenStep1", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code. " + entity);
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(RegisterResponseParam.CLIENT_ID.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET.toString()));
            assertTrue(jsonObj.has(RegisterResponseParam.REGISTRATION_ACCESS_TOKEN.toString()));
            assertTrue(jsonObj.has(REGISTRATION_CLIENT_URI.toString()));
            assertTrue(jsonObj.has(CLIENT_ID_ISSUED_AT.toString()));
            assertTrue(jsonObj.has(CLIENT_SECRET_EXPIRES_AT.toString()));

            clientId2 = jsonObj.getString(RegisterResponseParam.CLIENT_ID.toString());
            clientSecret2 = jsonObj.getString(RegisterResponseParam.CLIENT_SECRET.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        }
    }

    @Parameters({"authorizePath", "userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestLongLivedAccessTokenStep1")
    public void requestLongLivedAccessTokenStep2(final String authorizePath, final String userId,
                                                 final String userSecret, final String redirectUri) throws Exception {
        final String state = UUID.randomUUID().toString();

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);
        responseTypes.add(ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId2, scopes,
                redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.getPrompts().add(Prompt.NONE);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        Builder request = ResteasyClientBuilder.newClient()
                .target(url.toString() + authorizePath + "?" + authorizationRequest.getQueryString()).request();
        request.header("Authorization", "Basic " + authorizationRequest.getEncodedCredentials());
        request.header("Accept", MediaType.TEXT_PLAIN);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestLongLivedAccessTokenStep2", response, entity);

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

                accessToken1 = params.get("access_token");
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Response URI is not well formed");
            }
        }
    }

    @Parameters({"tokenPath"})
    @Test(dependsOnMethods = {"requestLongLivedAccessTokenStep2"})
    public void requestLongLivedAccessTokenStep3(final String tokenPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + tokenPath).request();
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        TokenRequest tokenRequest = new TokenRequest(GrantType.OXAUTH_EXCHANGE_TOKEN);
        tokenRequest.setOxAuthExchangeToken(accessToken1);
        tokenRequest.setAuthUsername(clientId2);
        tokenRequest.setAuthPassword(clientSecret2);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(tokenRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestLongLivedAccessTokenStep3", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
                "Unexpected result: " + response.getHeaderString("Pragma"));
        assertNotNull(entity, "Unexpected result: " + entity);
        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has("access_token"), "Unexpected result: access_token not found");
            assertTrue(jsonObj.has("token_type"), "Unexpected result: token_type not found");
            assertTrue(jsonObj.has("expires_in"), "Unexpected result: expires_in not found");

            accessToken2 = jsonObj.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage() + "\nResponse was: " + entity);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Parameters({"userInfoPath"})
    @Test(dependsOnMethods = "requestLongLivedAccessTokenStep3")
    public void requestLongLivedAccessTokenStep4(final String userInfoPath) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(url.toString() + userInfoPath).request();
        request.header("Authorization", "Bearer " + accessToken2);
        request.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        UserInfoRequest userInfoRequest = new UserInfoRequest(null);

        Response response = request
                .post(Entity.form(new MultivaluedHashMap<String, String>(userInfoRequest.getParameters())));
        String entity = response.readEntity(String.class);

        showResponse("requestLongLivedAccessTokenStep4", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        assertTrue(
                response.getHeaderString("Cache-Control") != null
                        && response.getHeaderString("Cache-Control").equals("no-store, private"),
                "Unexpected result: " + response.getHeaderString("Cache-Control"));
        assertTrue(response.getHeaderString("Pragma") != null && response.getHeaderString("Pragma").equals("no-cache"),
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

}
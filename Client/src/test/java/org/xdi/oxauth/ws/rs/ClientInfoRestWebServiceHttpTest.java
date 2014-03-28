package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Functional tests for Client Info Web Services (HTTP)
 *
 * @author Javier Rojas Blum Date: 07.20.2012
 */
public class ClientInfoRestWebServiceHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestClientInfoImplicitFlow(final String userId, final String userSecret,
                                              final String clientId, final String redirectUri) throws Exception {
        showTitle("requestClientInfoImplicitFlow");

        // 1. Request authorization
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);
        responseTypes.add(ResponseType.ID_TOKEN);
        List<String> scopes = new ArrayList<String>();
        scopes.add("clientinfo");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse.getScope(), "The scope must be null");
        assertNotNull(authorizationResponse.getIdToken(), "The id token must be null");

        String accessToken = authorizationResponse.getAccessToken();

        // 2. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
        assertNotNull(clientInfoResponse.getClaim("oxAuthAppType"), "Unexpected result: oxAuthAppType not found");
        assertNotNull(clientInfoResponse.getClaim("oxAuthIdTokenSignedResponseAlg"), "Unexpected result: oxAuthIdTokenSignedResponseAlg not found");
        assertNotNull(clientInfoResponse.getClaim("oxAuthRedirectURI"), "Unexpected result: oxAuthRedirectURI not found");
        assertNotNull(clientInfoResponse.getClaim("oxAuthScope"), "Unexpected result: oxAuthScope not found");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret"})
    @Test
    public void requestClientInfoPasswordFlow(
            final String userId, final String userSecret,
            final String clientId, final String clientSecret) throws Exception {
        showTitle("requestClientInfoPasswordFlow");

        // 1. Request authorization
        String username = userId;
        String password = userSecret;
        String scope = "clientinfo";

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        TokenResponse response1 = tokenClient.execResourceOwnerPasswordCredentialsGrant(username, password, scope,
                clientId, clientSecret);

        showClient(tokenClient);
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getRefreshToken(), "The refresh token is null");
        assertNotNull(response1.getScope(), "The scope is null");

        String accessToken = response1.getAccessToken();

        // 2. Request client info
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse response2 = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(response2.getStatus(), 200, "Unexpected response code: " + response2.getStatus());
        assertNotNull(response2.getClaim("displayName"), "Unexpected result: displayName not found");
        assertNotNull(response2.getClaim("inum"), "Unexpected result: inum not found");
        assertNotNull(response2.getClaim("oxAuthAppType"), "Unexpected result: oxAuthAppType not found");
        assertNotNull(response2.getClaim("oxAuthIdTokenSignedResponseAlg"), "Unexpected result: oxAuthIdTokenSignedResponseAlg not found");
        assertNotNull(response2.getClaim("oxAuthRedirectURI"), "Unexpected result: oxAuthRedirectURI not found");
        assertNotNull(response2.getClaim("oxAuthScope"), "Unexpected result: oxAuthScope not found");
    }

    @Test
    public void requestClientInfoInvalidRequest() throws Exception {
        showTitle("requestClientInfoInvalidRequest");

        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse response = clientInfoClient.execClientInfo(null);

        showClient(clientInfoClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(response.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Test
    public void requestClientInfoInvalidToken() throws Exception {
        showTitle("requestClientInfoInvalidToken");

        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse response = clientInfoClient.execClientInfo("INVALID-TOKEN");

        showClient(clientInfoClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(response.getErrorDescription(), "Unexpected result: errorDescription not found");
    }
}
package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Functional tests for End Session Web Services (HTTP)
 *
 * @author Javier Rojas Blum Date: 12.16.2011
 */
public class EndSessionRestWebServiceHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "postLogoutRedirectUri"})
    @Test
    public void requestEndSession(final String userId, final String userSecret, final String redirectUri,
                                  final String redirectUris, final String postLogoutRedirectUri) throws Exception {
        showTitle("requestEndSession");

        // 1. OpenID Connect Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN));
        registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);
        responseTypes.add(ResponseType.ID_TOKEN);
        List<String> scopes = new ArrayList<String>();
        scopes.add("openid");
        scopes.add("profile");
        scopes.add("address");
        scopes.add("email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getState(), "The state is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getExpiresIn(), "The expires in value is null");
        assertNotNull(response1.getScope(), "The scope must be null");

        String idToken = response1.getIdToken();

        // 3. End session
        EndSessionClient endSessionClient = new EndSessionClient(endSessionEndpoint);
        EndSessionResponse endSessionResponse = endSessionClient.execEndSession(idToken, postLogoutRedirectUri);

        EndSessionRequest endSessionRequest = new EndSessionRequest(idToken, postLogoutRedirectUri);

        showClient(endSessionClient);
        assertEquals(endSessionResponse.getStatus(), 302, "Unexpected response code: " + endSessionResponse.getStatus());
        assertNotNull(endSessionResponse.getLocation(), "The location is null");
    }

    @Test
    public void requestEndSessionFail1() throws Exception {
        showTitle("requestEndSessionFail1");

        EndSessionClient endSessionClient = new EndSessionClient(endSessionEndpoint);
        EndSessionResponse response = endSessionClient.execEndSession(null, null);

        showClient(endSessionClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    @Parameters({"postLogoutRedirectUri"})
    @Test
    public void requestEndSessionFail2(final String postLogoutRedirectUri) throws Exception {
        showTitle("requestEndSessionFail2");

        String state = "xyz";

        EndSessionClient endSessionClient = new EndSessionClient(endSessionEndpoint);
        EndSessionResponse response = endSessionClient.execEndSession("INVALID_ACCESS_TOKEN", postLogoutRedirectUri);

        showClient(endSessionClient);
        assertEquals(response.getStatus(), 401, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }
}
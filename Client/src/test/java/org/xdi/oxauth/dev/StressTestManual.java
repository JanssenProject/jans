package org.xdi.oxauth.dev;

import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.ResponseType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/08/2012
 */

public class StressTestManual {

    private static final String CLIENT_ID = "@!1111!0008!0074.74C0";
    private static final String REDIRECT_URI = "https://client.example.com/cb";
    private static final String AUTHORIZE_URL = "http://localhost:8085/seam/resource/restv1/oxauth/authorize";
    private static final String END_SESSION_URL = "http://localhost:8085/seam/resource/restv1/oxauth/end_session";
    private static final String POST_LOGOUT_REDIRECT_URI = "https://client.example.com/pl";
    private static final String USER_ID = "test_user";
    private static final String SECRET = "test";

    @Test(threadPoolSize = 10, invocationCount = 100000, timeOut = 10000)
    public void authCode() {
        // 1. Request authorization
        final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);

        final List<String> scopes = new ArrayList<String>();
        scopes.add("openid");
        scopes.add("profile");
        scopes.add("address");
        scopes.add("email");
        final String nonce = UUID.randomUUID().toString();
        final String state = "af0ifjsldkj";

        final AuthorizationRequest request = new AuthorizationRequest(responseTypes, CLIENT_ID, scopes, REDIRECT_URI, nonce);
        request.setState(state);
        request.setAuthUsername(USER_ID);
        request.setAuthPassword(SECRET);

        final AuthorizeClient authorizeClient = new AuthorizeClient(AUTHORIZE_URL);
        authorizeClient.setRequest(request);
        final AuthorizationResponse response1 = authorizeClient.exec();

        BaseTest.showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getState(), "The state is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getExpiresIn(), "The expires in value is null");
        assertNotNull(response1.getScope(), "The scope must be null");

        final String idToken = response1.getIdToken();

        // 2. End session
        final EndSessionClient endSessionClient = new EndSessionClient(END_SESSION_URL);
        final EndSessionResponse response2 = endSessionClient.execEndSession(idToken, POST_LOGOUT_REDIRECT_URI);

        BaseTest.showClient(endSessionClient);
        assertEquals(response2.getStatus(), 302, "Unexpected response code: " + response2.getStatus());
        assertNotNull(response2.getLocation(), "The location is null");
    }
}

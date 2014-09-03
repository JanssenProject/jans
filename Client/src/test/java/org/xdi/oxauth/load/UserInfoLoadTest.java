package org.xdi.oxauth.load;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.client.UserInfoClient;
import org.xdi.oxauth.client.UserInfoResponse;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.jwt.JwtClaimName;

/**
 * DON'T INCLUDE IT IN TEST SUITE.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/12/2013
 */

public class UserInfoLoadTest extends BaseTest {

    @Parameters({"userId", "userSecret", "clientId", "redirectUri"})
    @Test(invocationCount = 1000, threadPoolSize = 100)
    public void requestUserInfoImplicitFlow(final String userId, final String userSecret,
                                            final String clientId, final String redirectUri) throws Exception {
        showTitle("requestUserInfoImplicitFlow");

        // 1. Request authorization
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
        assertNotNull(response1.getIdToken(), "The id token must be null");

        String accessToken = response1.getAccessToken();

        // 2. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response2.getStatus(), 200, "Unexpected response code: " + response2.getStatus());
        assertNotNull(response2.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response2.getClaim(JwtClaimName.NAME));
        assertNotNull(response2.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response2.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response2.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response2.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response2.getClaim(JwtClaimName.LOCALE));
    }
}

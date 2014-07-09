package org.xdi.oxauth.load.benchmark;

import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/07/2014
 */

@Listeners({ BenchmarkTestListener.class })
public class BenchmarkAuthorizatoinRequests {

    // Think twice before invoking this test ;). Leads to OpenDJ (Berkley DB) failure
    // Caused by: LDAPSearchException(resultCode=80 (other), numEntries=0, numReferences=0, errorMessage='Database exception: (JE 4.1.10) JAVA_ERROR: Java Error occurred, recovery may not be possible.')
    // http://ox.gluu.org/doku.php?id=oxauth:profiling#obtain_access_token_-_2000_invocations_within_200_concurrent_threads
    @Parameters({"userId", "userSecret", "redirectUri", "clientId"})
    @Test(invocationCount = 10000, threadPoolSize = 300)
//    @Test
    public void test(final String userId, final String userSecret, String redirectUri, String clientId) {

        // hardcode -> we don't want to loose time on discover call
        String authorizationEndpoint = "https://localhost:8443/seam/resource/restv1/oxauth/authorize";

        final List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.TOKEN);
        responseTypes.add(ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = "STATE_XYZ";
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setState(state);
        request.setNonce(nonce);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getAccessToken(), "The access token is null");
        assertNotNull(response.getState(), "The state is null");
        assertNotNull(response.getTokenType(), "The token type is null");
        assertNotNull(response.getExpiresIn(), "The expires in value is null");
        assertNotNull(response.getScope(), "The scope must be null");
    }
}

package io.jans.ca.server;

import com.google.common.collect.Lists;
import io.jans.as.client.AuthorizationResponse;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.SeleniumTestUtils;
import io.jans.ca.common.response.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.AssertJUnit.*;

public class SpontaneousScopeAuthTest {

    @Parameters({"host", "opHost", "paramRedirectUrl", "userId", "userSecret"})
    @Test
    public void spontaneousScope(String host, String opHost, String paramRedirectUrl, String userId, String userSecret) throws Exception {

        List<String> spontaneousScopes = Lists.newArrayList("^transaction:.+$");
        List<String> responseTypes = Lists.newArrayList("code", "id_token", "token");
        List<String> scopes = Lists.newArrayList("openid", "profile", "address", "email", "phone", "user_name");

        RegisterSiteResponse registerResponse = registerClient(host, opHost, paramRedirectUrl, scopes, responseTypes, spontaneousScopes);

        // Request authorization and receive the authorization code.
        List<String> scopesWithSpontanious = Lists.newArrayList("openid", "profile", "address", "email", "phone", "user_name",
                "transaction:245", "transaction:8645");

        AuthorizationResponse authorizationResponse = requestAuthorization(opHost, userId, userSecret, paramRedirectUrl, responseTypes, scopesWithSpontanious, registerResponse.getClientId());

        final String[] responseScopes = authorizationResponse.getScope().split(" ");

        // Validate spontaneous scopes are present
        assertTrue(Arrays.asList(responseScopes).contains("transaction:245"));
        assertTrue(Arrays.asList(responseScopes).contains("transaction:8645"));
        assertFalse(Arrays.asList(responseScopes).contains("transaction:not_requested"));
    }

    private RegisterSiteResponse registerClient(String host, String opHost, String redirectUrls, List<String> scopes, List<String> responseTypes, List<String> spontaneousScopes) {

        ClientInterface client = Tester.newClient(host);
        // 1. allow spontaneous scopes (off by default)
        // 2. set spontaneous scope regular expression. In this example `transaction:345236456`
        final RegisterSiteResponse registerResponse = RegisterSiteTest.registerSite(client, opHost, redirectUrls, scopes, responseTypes, true, spontaneousScopes);

        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());
        return registerResponse;
    }

    private AuthorizationResponse requestAuthorization(final String opHost, final String userId, final String userSecret, final String redirectUri,
                                                       List<String> responseTypes, List<String> scopesWithSpontanious, String clientId) {
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = SeleniumTestUtils.authorizeClient(opHost, userId, userSecret, clientId, redirectUri, state, nonce, responseTypes, scopesWithSpontanious);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        return authorizationResponse;
    }
}

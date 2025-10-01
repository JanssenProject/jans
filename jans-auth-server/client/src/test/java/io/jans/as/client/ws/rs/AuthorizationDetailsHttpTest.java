package io.jans.as.client.ws.rs;

import com.google.common.collect.Lists;
import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.model.common.*;
import io.jans.as.model.register.ApplicationType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Authorization Details Http Tests consists of:
 * 1. register client
 * 2. send authorization request
 * 3. login&authorize with Authorization Code Flow
 * 4. get token at Token Endpoint and check response has authorization_details
 * 5. Request user info
 * 6. introspect access_token and check authorization_details are present
 *
 * "demo_authz_detail" is authorization details type which corresponds to demo AuthzDetail custom script.
 *
 * @author Yuriy Z
 */
public class AuthorizationDetailsHttpTest extends BaseTest {

    private static final String AUTHORIZATION_DETAILS = "[\n" +
            "  {\n" +
            "    \"type\": \"demo_authz_detail\",\n" +
            "    \"actions\": [\n" +
            "      \"list_accounts\",\n" +
            "      \"read_balances\"\n" +
            "    ],\n" +
            "    \"locations\": [\n" +
            "      \"https://example.com/accounts\"\n" +
            "    ],\n" +
            "    \"ui_representation\": \"Read balances and list accounts at https://example.com/accounts\"\n" +
            "  }\n" +
            "]";

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void authorizationWithAuthorizationDetails(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws Exception {
        showTitle("authorizationWithAuthorizationDetails");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();

        // 3. login&authorize with Authorization Code Flow
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        AssertBuilder.authorizationResponse(authorizationResponse)
                .check();

        // 4. Request access token using the authorization code and check response has authorization_details
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setAuthorizationDetails(AUTHORIZATION_DETAILS);

        TokenClient tokenClient1 = newTokenClient(tokenRequest);
        tokenClient1.setRequest(tokenRequest);
        TokenResponse tokenResponse1 = tokenClient1.exec();

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(tokenResponse1)
                .check();

        final String accessToken = tokenResponse1.getAccessToken();

        // 5. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setExecutor(clientEngine(true));
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .check();

        // 6. introspect access_token and check authorization_details are present
        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint);
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken("Bearer " + accessToken, accessToken);

        assertNotNull(introspectionResponse);
        assertTrue(introspectionResponse.isActive());
        assertNotNull(introspectionResponse.getAuthorizationDetails());
        assertTrue(AuthzDetails.of(introspectionResponse.getAuthorizationDetails().toString()).similar(AUTHORIZATION_DETAILS));

        System.out.println("Introspection response for access_token: " + accessToken);
        System.out.println(introspectionResponse);
    }

    public RegisterResponse registerClient(final String redirectUris, List<ResponseType> responseTypes, List<String> scopes) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                io.jans.as.model.util.StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(List.of(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT));
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setAuthorizationDetailsTypes(Lists.newArrayList("demo_authz_detail"));

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {


        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthorizationDetails(AUTHORIZATION_DETAILS);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        return authorizationResponse;
    }

}

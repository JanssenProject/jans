package io.jans.as.client.ws.rs.session;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.*;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.register.ApplicationType;
import io.jans.model.tokenstatus.StatusList;
import io.jans.model.tokenstatus.TokenStatus;
import io.jans.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Z
 */
public class SessionStatusListHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void sessionStatusList(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        showTitle("sessionStatusList");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name", "revoke_session");

        // 1. Register client
        Pair<String, String> clientIdAndSecret = getOrRegisterClient(redirectUris, responseTypes, scopes);

        String clientId = clientIdAndSecret.getFirst();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String sessionJwtString = authorizationResponse.getSessionJwt();
        assertNotNull("Session JWT must not be null.", sessionJwtString);

        System.out.println("sessionStatusList - session_jwt");
        System.out.println(sessionJwtString);
        Jwt sessionJwt = Jwt.parseOrThrow(sessionJwtString);
        final int sessionIndex = sessionJwt.getClaims().getClaimAsJSON("status_list").getInt("idx");
        System.out.println("sessionStatusList - SESSION_JWT idx: " + sessionIndex);

        assertEquals(TokenStatus.VALID, loadStatus(sessionIndex));

        revokeSession(clientIdAndSecret, userId);

        System.out.println("sessionStatusList - SESSION_JWT idx: " + sessionIndex);  // re-print for convenience
        // give time to let status went to list
        Thread.sleep(2000);
        assertEquals(TokenStatus.INVALID, loadStatus(sessionIndex));
    }

    private void revokeSession(Pair<String, String> clientIdAndSecret, String userId) {
        String clientId = clientIdAndSecret.getFirst();
        String clientSecret = clientIdAndSecret.getSecond();

        RevokeSessionRequest revokeSessionRequest = new RevokeSessionRequest("uid", userId);
        revokeSessionRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        revokeSessionRequest.setAuthUsername(clientId); // it must be client with revoke_session scope
        revokeSessionRequest.setAuthPassword(clientSecret);

        RevokeSessionClient revokeSessionClient = newRevokeSessionClient(revokeSessionRequest);
        RevokeSessionResponse revokeSessionResponse = revokeSessionClient.exec();

        showClient(revokeSessionClient);

        Assert.assertEquals(revokeSessionResponse.getStatus(), 200);
    }

    private TokenStatus loadStatus(int index) throws IOException {
        StatusListRequest statusListRequest = new StatusListRequest();
        StatusListClient statusListClient = new StatusListClient(sessionStatusListEndpoint);
        StatusListResponse statusListResponse = statusListClient.exec(statusListRequest);
        showClient(statusListClient);
        System.out.println(String.format("bits: %s, lst: %s", statusListResponse.getBits(), statusListResponse.getLst()));

        StatusList statusList = statusListResponse.getStatusList();
        final int status = statusList.get(index);
        return TokenStatus.fromValue(status);
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setRequestSessionJwt(true);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        return authorizationResponse;
    }

    public Pair<String, String> getOrRegisterClient(final String redirectUris, List<ResponseType> responseTypes, List<String> scopes) {
        final String clientId = System.getProperty("CLIENT_ID");
        final String clientSecret = System.getProperty("CLIENT_SECRET");
        if (StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(clientSecret)) {
            return new Pair<>(clientId, clientSecret);
        }

        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, scopes);

        return new Pair<>(registerResponse.getClientId(), registerResponse.getClientSecret());
    }

    public RegisterResponse registerClient(final String redirectUris, List<ResponseType> responseTypes, List<String> scopes) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                io.jans.as.model.util.StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setAccessTokenAsJwt(true);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        return registerResponse;
    }

    public static void main(String[] args) throws IOException {
        StatusList before = StatusList.fromEncoded("eNoDAAAAAAE", 2);
        StatusList after = StatusList.fromEncoded("eNoLYGEYhIAFADIjAFk", 2);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test(enabled = false)
    public void statusListPerformanceLoad(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            sessionStatusList(userId, userSecret, redirectUris, redirectUri);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test(enabled = false, invocationCount = 1000, threadPoolSize = 10)
    public void fullStressTest_sessionStatusList(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        showTitle("sessionStatusList");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name", "revoke_session");

        // 1. Register client
        Pair<String, String> clientIdAndSecret = getOrRegisterClient(redirectUris, responseTypes, scopes);

        String clientId = clientIdAndSecret.getFirst();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String sessionJwtString = authorizationResponse.getSessionJwt();
        assertNotNull("Session JWT must not be null.", sessionJwtString);

        System.out.println("sessionStatusList - session_jwt");
        System.out.println(sessionJwtString);
        Jwt sessionJwt = Jwt.parseOrThrow(sessionJwtString);
        final int sessionIndex = sessionJwt.getClaims().getClaimAsJSON("status_list").getInt("idx");
        System.out.println("sessionStatusList - SESSION_JWT idx: " + sessionIndex);
        final TokenStatus status = loadStatus(sessionIndex);
        System.out.println("sessionStatusList - SESSION_JWT idx: " + sessionIndex + ", status: " + status);

//        revokeSession(clientIdAndSecret, userId);
//
//        System.out.println("sessionStatusList - SESSION_JWT idx: " + sessionIndex);  // re-print for convenience
//        // give time to let status went to list
//        Thread.sleep(2000);
//        assertEquals(TokenStatus.INVALID, loadStatus(sessionIndex));
    }
}

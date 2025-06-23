package io.jans.as.client.ws.rs.logout;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.TestExecutionContext;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.common.TokenTypeHint;
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
import java.util.function.Function;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Z
 */
public class LogoutStatusJwtHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void logoutStatusJwt_whenRevokeTokenByFullToken_shouldBeInvalidInStatusList(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        showTitle("logoutStatusJwt_whenRevokeTokenByFullToken_shouldBeInvalidInStatusList");

        Function<TestExecutionContext, Void> revokeFunction = t -> {
            t.setTokenTypeHint(TokenTypeHint.LOGOUT_STATUS_JWT);

            revokeToken(new Pair<>(t.getClientId(), t.getClientSecret()), t.getToken(), t.getTokenTypeHint());
            return null;
        };

        logoutStatusJwtBasicTest(userId, userSecret, redirectUris, redirectUri, revokeFunction);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void logoutStatusJwt_whenRevokeTokenByJti_shouldBeInvalidInStatusList(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        showTitle("logoutStatusJwt_whenRevokeTokenByJti_shouldBeInvalidInStatusList");

        Function<TestExecutionContext, Void> revokeFunction = t -> {
            t.setTokenTypeHint(TokenTypeHint.JTI);

            try {
                Jwt jwt = Jwt.parseOrThrow(t.getToken());
                final String jti = jwt.getClaims().getClaimAsString("jti");
                assertNotNull(jti);

                revokeToken(new Pair<>(t.getClientId(), t.getClientSecret()), jti, t.getTokenTypeHint());
            } catch (InvalidJwtException e) {
                // force fail test
                Assert.fail("Failed to parse Logout Status JWT into JWT:" + t.getToken() + ", message: " + e.getMessage());
            }

            return null;
        };

        logoutStatusJwtBasicTest(userId, userSecret, redirectUris, redirectUri, revokeFunction);
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void logoutStatusJwt_whenRunGlobalTokenRevocation_shouldBeInvalidInStatusList(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        showTitle("logoutStatusJwt_whenRunGlobalTokenRevocation_shouldBeInvalidInStatusList");

        Function<TestExecutionContext, Void> revokeFunction = t -> {
            GlobalTokenRevocationClientRequest revocationRequest = new GlobalTokenRevocationClientRequest();
            revocationRequest.setFormat("uid");
            revocationRequest.setId(userId);
            revocationRequest.setAuthUsername(t.getClientId());
            revocationRequest.setAuthPassword(t.getClientSecret());
            revocationRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            GlobalTokenRevocationClient globalTokenRevocationClient = new GlobalTokenRevocationClient(globalTokenRevocationEndpoint);
            globalTokenRevocationClient.exec(revocationRequest);
            showClient(globalTokenRevocationClient);

            return null;
        };

        logoutStatusJwtBasicTest(userId, userSecret, redirectUris, redirectUri, revokeFunction);
    }

    public void logoutStatusJwtBasicTest(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri, Function<TestExecutionContext, Void> revokeFunction) throws IOException, InvalidJwtException, InterruptedException {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name", "revoke_any_token", "global_token_revocation");

        // 1. Register client
        Pair<String, String> clientIdAndSecret = getOrRegisterClient(redirectUris, responseTypes, scopes);

        String clientId = clientIdAndSecret.getFirst();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String logoutStatusJwtString = authorizationResponse.getLogoutStatusJwt();
        assertNotNull("Logout Status JWT must not be null.", logoutStatusJwtString);

        System.out.println("statusList - logout_status_jwt");
        System.out.println(logoutStatusJwtString);
        Jwt jwt = Jwt.parseOrThrow(logoutStatusJwtString);
        final int index = jwt.getClaims().getClaimAsJSON("status_list").getInt("idx");
        System.out.println("statusList - LOGOUT_STATUS_JWT idx: " + index);

        assertEquals(TokenStatus.VALID, loadStatus(index));

        TestExecutionContext testExecutionContext = new TestExecutionContext();
        testExecutionContext.setClientId(clientId);
        testExecutionContext.setClientSecret(clientIdAndSecret.getSecond());
        testExecutionContext.setToken(logoutStatusJwtString);

        revokeFunction.apply(testExecutionContext);

        System.out.println("statusList - LOGOUT_STATUS_JWT idx: " + index);  // re-print for convenience
        // give time to let status went to list
        Thread.sleep(2000);
        assertEquals(TokenStatus.INVALID, loadStatus(index));
    }

    private void revokeToken(Pair<String, String> clientIdAndSecret, String logoutStatusJwt, TokenTypeHint tokenTypeHint) {
        String clientId = clientIdAndSecret.getFirst();
        String clientSecret = clientIdAndSecret.getSecond();

        TokenRevocationRequest revocationRequest = new TokenRevocationRequest();
        revocationRequest.setToken(logoutStatusJwt);
        revocationRequest.setTokenTypeHint(tokenTypeHint);
        revocationRequest.setAuthUsername(clientId);
        revocationRequest.setAuthPassword(clientSecret);

        TokenRevocationClient revocationClient = new TokenRevocationClient(tokenRevocationEndpoint);
        revocationClient.setRequest(revocationRequest);

        TokenRevocationResponse revocationResponse = revocationClient.exec();

        showClient(revocationClient);
        assertEquals(200, revocationResponse.getStatus());
    }

    private TokenStatus loadStatus(int index) throws IOException {
        StatusListRequest statusListRequest = new StatusListRequest();
        StatusListClient statusListClient = new StatusListClient(statusListEndpoint);
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
        authorizationRequest.setRequestLogoutStatusJwt(true);

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
    public void logoutStatusJwtPerformanceLoad(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            logoutStatusJwt_whenRevokeTokenByFullToken_shouldBeInvalidInStatusList(userId, userSecret, redirectUris, redirectUri);
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test(enabled = false, invocationCount = 1000, threadPoolSize = 10)
    public void fullStressTest_logoutStatusJwtList(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        showTitle("fullStressTest_logoutStatusJwtList");

        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name", "revoke_any_token", "global_token_revocation");

        // 1. Register client
        Pair<String, String> clientIdAndSecret = getOrRegisterClient(redirectUris, responseTypes, scopes);

        String clientId = clientIdAndSecret.getFirst();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String logoutStatusJwtString = authorizationResponse.getLogoutStatusJwt();
        assertNotNull("Logout Status JWT must not be null.", logoutStatusJwtString);

        System.out.println("statusList - logout_status_jwt");
        System.out.println(logoutStatusJwtString);
        Jwt logoutStatusJwt = Jwt.parseOrThrow(logoutStatusJwtString);
        final int index = logoutStatusJwt.getClaims().getClaimAsJSON("status_list").getInt("idx");
        System.out.println("statusList - LOGOUT_STATUS_JWT idx: " + index);
        final TokenStatus status = loadStatus(index);
        System.out.println("statusList - LOGOUT_STATUS_JWT idx: " + index + ", status: " + status);

//        revokeToken(clientIdAndSecret, logoutStatusJwtString);
//
//        System.out.println("statusList - LOGOUT_STATUS_JWT idx: " + sessionIndex);  // re-print for convenience
//        // give time to let status went to list
//        Thread.sleep(2000);
//        assertEquals(TokenStatus.INVALID, loadStatus(sessionIndex));
    }
}

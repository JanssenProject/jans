package io.jans.as.client.ws.rs.token;

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
import java.util.List;
import java.util.UUID;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Yuriy Z
 */
public class StatusListHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri"})
    @Test
    public void statusList(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri) throws IOException, InvalidJwtException, InterruptedException {
        showTitle("statusList");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        // 1. Register client
        Pair<String, String> clientIdAndSecret = getOrRegisterClient(redirectUris, responseTypes, scopes);

        String clientId = clientIdAndSecret.getFirst();
        String clientSecret = clientIdAndSecret.getSecond();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String authorizationCode = authorizationResponse.getCode();

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        System.out.println("statusList - ACCESS_TOKEN");
        System.out.println(tokenResponse.getAccessToken());
        Jwt accessTokenJwt = Jwt.parseOrThrow(tokenResponse.getAccessToken());
        final int accessTokenIndex = accessTokenJwt.getClaims().getClaimAsJSON("status").getJSONObject("status_list").getInt("idx");
        System.out.println("statusList - ACCESS_TOKEN idx: " + accessTokenIndex);

        assertEquals(TokenStatus.VALID, loadStatus(accessTokenIndex));

        revokeAccessToken(clientIdAndSecret, tokenResponse.getAccessToken());

        System.out.println("statusList - ACCESS_TOKEN idx: " + accessTokenIndex);  // re-print for convenience
        // give time to let status went to list
        Thread.sleep(2000);
        assertEquals(TokenStatus.INVALID, loadStatus(accessTokenIndex));
    }

    private void revokeAccessToken(Pair<String, String> clientIdAndSecret, String accessToken) {
        String clientId = clientIdAndSecret.getFirst();
        String clientSecret = clientIdAndSecret.getSecond();

        TokenRevocationRequest revocationRequest = new TokenRevocationRequest();
        revocationRequest.setToken(accessToken);
        revocationRequest.setTokenTypeHint(TokenTypeHint.ACCESS_TOKEN);
        revocationRequest.setAuthUsername(clientId);
        revocationRequest.setAuthPassword(clientSecret);

        TokenRevocationClient revocationClient = new TokenRevocationClient(tokenRevocationEndpoint);
        revocationClient.setRequest(revocationRequest);

        TokenRevocationResponse revocationResponse = revocationClient.exec();

        showClient(revocationClient);
        Assert.assertEquals(revocationResponse.getStatus(), 200, "Unexpected response code: " + revocationResponse.getStatus());
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
        AssertBuilder.registerResponse(registerResponse).created().check();
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
            statusList(userId, userSecret, redirectUris, redirectUri);
        }
    }
}

package org.gluu.oxd.server;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.client.GetTokensByCodeResponse2;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.SeleniumTestUtils;
import org.gluu.oxd.common.params.GetAccessTokenByRefreshTokenParams;
import org.gluu.oxd.common.params.GetAuthorizationCodeParams;
import org.gluu.oxd.common.params.GetClientTokenParams;
import org.gluu.oxd.common.params.GetTokensByCodeParams;
import org.gluu.oxd.common.response.GetClientTokenResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;

import static org.gluu.oxd.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetTokensByCodeTest {

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void whenValidCodeIsUsed_shouldGetTokenInResponse(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
        refreshToken(tokensResponse, client, site);
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void withAuthenticationMethod_shouldGetTokenInResponse(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite_withAuthenticationMethod(client, opHost, redirectUrls, "PS256", AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), AuthenticationMethod.PRIVATE_KEY_JWT.toString(), "PS256");
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withHS256(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "HS256");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withHS384(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "HS384");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withHS512(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "HS512");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withRS256(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "RS256");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withRS384(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "RS384");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withRS512(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "RS512");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withES256(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "ES256");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withES384(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "ES384");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withES512(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "ES512");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withPS256(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "PS256");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withPS384(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "PS384");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withPS512(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "PS512");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withNoneAlgo(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "none");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void whenInvalidCodeIsUsed_shouldGet400BadRequest(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        tokenByInvalidCode(client, site, userId, userSecret, CoreUtils.secureRandomString());
    }

    public static GetClientTokenResponse refreshToken(GetTokensByCodeResponse2 resp, ClientInterface client, RegisterSiteResponse site) {
        notEmpty(resp.getRefreshToken());

        // refresh token
        final GetAccessTokenByRefreshTokenParams refreshParams = new GetAccessTokenByRefreshTokenParams();
        refreshParams.setOxdId(site.getOxdId());
        refreshParams.setScope(Lists.newArrayList("openid", "oxd"));
        refreshParams.setRefreshToken(resp.getRefreshToken());

        GetClientTokenResponse refreshResponse = client.getAccessTokenByRefreshToken(Tester.getAuthorization(site), null, refreshParams);

        assertNotNull(refreshResponse);
        notEmpty(refreshResponse.getAccessToken());
        notEmpty(refreshResponse.getRefreshToken());
        return refreshResponse;
    }

    public static GetTokensByCodeResponse2 tokenByCode(ClientInterface client, RegisterSiteResponse site, String opHost, String userId, String userSecret, String clientId, String redirectUrls, String nonce) {
        return tokenByCode(client, site, opHost, userId, userSecret, clientId, redirectUrls, nonce, null, null);
    }

    public static GetTokensByCodeResponse2 tokenByCode(ClientInterface client, RegisterSiteResponse site, String opHost, String userId, String userSecret, String clientId, String redirectUrls, String nonce, String authenticationMethod, String algorithm) {

        final String state = CoreUtils.secureRandomString();
        RegisterSiteResponse authServer = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        String accessToken = Tester.getAuthorization(authServer);
        String authorizationOxdId = authServer.getOxdId();

        String code = codeRequest(client, opHost, site, userId, userSecret, clientId, redirectUrls, state, nonce, accessToken, authorizationOxdId);

        notEmpty(code);

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(code);
        params.setState(state);
        params.setAuthenticationMethod(authenticationMethod);
        params.setAlgorithm(algorithm);

        final GetTokensByCodeResponse2 resp = client.getTokenByCode(accessToken, authorizationOxdId, params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        return resp;
    }

    public static GetTokensByCodeResponse2 tokenByInvalidCode(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret, String nonce) {

        final String state = CoreUtils.secureRandomString();
        //codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce);

        final String code = CoreUtils.secureRandomString();

        String testOxdId = site.getOxdId();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(testOxdId);
        params.setCode(code);
        params.setState(state);

        GetTokensByCodeResponse2 resp = null;

        try {
            resp = client.getTokenByCode(Tester.getAuthorization(site), null, params);
            assertNotNull(resp);
            notEmpty(resp.getAccessToken());
            notEmpty(resp.getIdToken());
            notEmpty(resp.getRefreshToken());

        } catch (BadRequestException ex) {
            assertEquals(ex.getMessage(), "HTTP 400 Bad Request");
        }

        return resp;
    }

    public static String codeRequest(ClientInterface client, String opHost, RegisterSiteResponse site, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce, String accessToken, String authorizationOxdId) {
        SeleniumTestUtils.authorizeClient(opHost, userId, userSecret, clientId, redirectUrls, state, nonce, null, null);
        GetAuthorizationCodeParams params = new GetAuthorizationCodeParams();
        params.setOxdId(site.getOxdId());
        params.setUsername(userId);
        params.setPassword(userSecret);
        params.setState(state);
        params.setNonce(nonce);

        return client.getAuthorizationCode(accessToken, authorizationOxdId, params).getCode();
    }
}

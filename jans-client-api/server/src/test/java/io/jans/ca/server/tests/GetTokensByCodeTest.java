package io.jans.ca.server.tests;

import com.google.common.collect.Lists;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.util.Util;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.GetTokensByCodeResponse2;
import io.jans.ca.common.CoreUtils;
import io.jans.ca.common.SeleniumTestUtils;
import io.jans.ca.common.params.GetAccessTokenByRefreshTokenParams;
import io.jans.ca.common.params.GetAuthorizationCodeParams;
import io.jans.ca.common.params.GetTokensByCodeParams;
import io.jans.ca.common.response.GetClientTokenResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.codec.binary.Base64;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.util.Strings;

import java.net.URI;

import static io.jans.ca.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetTokensByCodeTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void whenValidCodeIsUsed_shouldGetTokenInResponse(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
        refreshToken(tokensResponse, client, site);
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void withbase64urlencodeState_shouldGetTokenInResponse(String host, String opHost, String redirectUrls, String userId, String userSecret) throws Exception {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        String state = Base64.encodeBase64String(Util.getBytes("https://www.gluu,org"));
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), state);
        refreshToken(tokensResponse, client, site);
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void withAuthenticationMethod_shouldGetTokenInResponse(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite_withAuthenticationMethod(client, opHost, redirectUrls, "PS256", AuthenticationMethod.PRIVATE_KEY_JWT.toString());
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString(), AuthenticationMethod.PRIVATE_KEY_JWT.toString(), "PS256");
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withHS256(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "HS256");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withHS384(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "HS384");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withHS512(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "HS512");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret", "opConfigurationEndpoint"})
    @Test
    public void getToken_withRS256(String host, String opHost, String redirectUrls, String userId, String userSecret, String opConfigurationEndpoint) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "RS256", opConfigurationEndpoint);
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withRS384(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "RS384");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret", "opConfigurationEndpoint"})
    @Test
    public void getToken_withRS512(String host, String opHost, String redirectUrls, String userId, String userSecret, String opConfigurationEndpoint) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "RS512", opConfigurationEndpoint);
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret", "opConfigurationEndpoint"})
    @Test
    public void getToken_withES256(String host, String opHost, String redirectUrls, String userId, String userSecret, String opConfigurationEndpoint) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "ES256", opConfigurationEndpoint);
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withES384(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "ES384");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withES512(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "ES512");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withPS256(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "PS256");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withPS384(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "PS384");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withPS512(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "PS512");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void getToken_withNoneAlgo(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, "none");
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, opHost, userId, userSecret, site.getClientId(), redirectUrls, CoreUtils.secureRandomString(), CoreUtils.secureRandomString());
    }

    @Parameters({"host", "opHost", "redirectUrls", "userId", "userSecret"})
    @Test
    public void whenInvalidCodeIsUsed_shouldGet400BadRequest(String host, String opHost, String redirectUrls, String userId, String userSecret) {
        ClientInterface client = getClientInterface(url);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        tokenByInvalidCode(client, site, userId, userSecret, CoreUtils.secureRandomString());
    }

    public static GetClientTokenResponse refreshToken(GetTokensByCodeResponse2 resp, ClientInterface client, RegisterSiteResponse site) {
        notEmpty(resp.getRefreshToken());

        // refresh token
        final GetAccessTokenByRefreshTokenParams refreshParams = new GetAccessTokenByRefreshTokenParams();
        refreshParams.setRpId(site.getRpId());
        refreshParams.setScope(Lists.newArrayList("openid", "jans_client_api"));
        refreshParams.setRefreshToken(resp.getRefreshToken());

        GetClientTokenResponse refreshResponse = client.getAccessTokenByRefreshToken(Tester.getAuthorization(client.getApitargetURL(), site), null, refreshParams);

        assertNotNull(refreshResponse);
        notEmpty(refreshResponse.getAccessToken());
        notEmpty(refreshResponse.getRefreshToken());
        return refreshResponse;
    }

    public static GetTokensByCodeResponse2 tokenByCode(ClientInterface client, RegisterSiteResponse site, String opHost, String userId, String userSecret, String clientId, String redirectUrls, String nonce, String state) {
        return tokenByCode(client, site, opHost, userId, userSecret, clientId, redirectUrls, nonce, state, null, null);
    }

    public static GetTokensByCodeResponse2 tokenByCode(ClientInterface client, RegisterSiteResponse site, String opHost, String userId, String userSecret, String clientId, String redirectUrls, String nonce, String state, String authenticationMethod, String algorithm) {

        RegisterSiteResponse authServer = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        String accessToken = Tester.getAuthorization(client.getApitargetURL(), authServer);
        String authorizationRpId = authServer.getRpId();

        String code = codeRequest(client, opHost, site, userId, userSecret, clientId, redirectUrls, state, nonce, accessToken, authorizationRpId);

        notEmpty(code);

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setRpId(site.getRpId());
        params.setCode(code);
        params.setState(state);
        params.setAuthenticationMethod(authenticationMethod);
        params.setAlgorithm(algorithm);

        final GetTokensByCodeResponse2 resp = client.getTokenByCode(accessToken, authorizationRpId, params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        return resp;
    }

    public static GetTokensByCodeResponse2 tokenByInvalidCode(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret, String nonce) {

        final String state = CoreUtils.secureRandomString();
        //codeRequest(client, site.getRpId(), userId, userSecret, state, nonce);

        final String code = CoreUtils.secureRandomString();

        String testRpId = site.getRpId();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setRpId(testRpId);
        params.setCode(code);
        params.setState(state);

        GetTokensByCodeResponse2 resp = null;

        resp = client.getTokenByCode(Tester.getAuthorization(client.getApitargetURL(), site), null, params);
        assertNotNull(resp);
        assertEquals(resp.getError(), "bad_request");
        assertEquals(resp.getErrorDescription(), "'state' is not registered.");

        return resp;
    }

    public static String codeRequest(ClientInterface client, String opHost, RegisterSiteResponse site, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce) {
        return codeRequest(client, opHost, site, userId, userSecret, clientId, redirectUrls, state, nonce, null, null);
    }

    public static String codeRequest(ClientInterface client, String opHost, RegisterSiteResponse site, String userId, String userSecret, String clientId, String redirectUrls, String state, String nonce, String accessToken, String authorizationRpId) {
        SeleniumTestUtils.authorizeClient(opHost, userId, userSecret, clientId, redirectUrls, state, nonce, null, null);
        GetAuthorizationCodeParams params = new GetAuthorizationCodeParams();
        params.setRpId(site.getRpId());
        params.setUsername(userId);
        params.setPassword(userSecret);
        params.setState(state);
        params.setNonce(nonce);

        if (Strings.isNullOrEmpty(accessToken)) {
            accessToken = Tester.getAuthorization(client.getApitargetURL(), site);
            System.out.println("----------ACCESS TOKEN " + accessToken);
        }
        return client.getAuthorizationCode(accessToken, authorizationRpId, params).getCode();
    }
}

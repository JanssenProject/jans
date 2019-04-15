package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.client.GetTokensByCodeResponse2;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.params.GetAccessTokenByRefreshTokenParams;
import org.gluu.oxd.common.params.GetAuthorizationCodeParams;
import org.gluu.oxd.common.params.GetTokensByCodeParams;
import org.gluu.oxd.common.response.GetClientTokenResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.gluu.oxd.server.TestUtils.notEmpty;
import static org.junit.Assert.assertEquals;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetTokensByCodeTest {

    @Parameters({"host", "opHost", "redirectUrl"})
    @BeforeClass
    public static void beforeClass(String host, String opHost, String redirectUrl) {
        SetUpTest.beforeSuite(host, opHost, redirectUrl);
    }

    @AfterClass
    public static void afterClass() {
        SetUpTest.afterSuite();
    }


    @Parameters({"host", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void whenValidCodeIsUsed_shouldGetTokenInResponse(String host, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        GetTokensByCodeResponse2 tokensResponse = tokenByCode(client, site, userId, userSecret, CoreUtils.secureRandomString());
        refreshToken(tokensResponse, client, site.getOxdId());
    }



    @Parameters({"host", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void whenInvalidCodeIsUsed_shouldGet400BadRequest(String host, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        tokenByInvalidCode(client, site, userId, userSecret, CoreUtils.secureRandomString());
    }




    public static GetClientTokenResponse refreshToken(GetTokensByCodeResponse2 resp, ClientInterface client, String oxdId) {
        notEmpty(resp.getRefreshToken());

        // refresh token
        final GetAccessTokenByRefreshTokenParams refreshParams = new GetAccessTokenByRefreshTokenParams();
        refreshParams.setOxdId(oxdId);
        refreshParams.setScope(Lists.newArrayList("openid"));
        refreshParams.setRefreshToken(resp.getRefreshToken());
        refreshParams.setProtectionAccessToken(Tester.getAuthorization());

        GetClientTokenResponse refreshResponse = client.getAccessTokenByRefreshToken(Tester.getAuthorization(), refreshParams);

        assertNotNull(refreshResponse);
        notEmpty(refreshResponse.getAccessToken());
        notEmpty(refreshResponse.getRefreshToken());
        return refreshResponse;
    }

    public static GetTokensByCodeResponse2 tokenByCode(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret, String nonce) {

        final String state = CoreUtils.secureRandomString();

        String code = codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce);

        notEmpty(code);

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(code);
        params.setState(state);

        final GetTokensByCodeResponse2 resp = client.getTokenByCode(Tester.getAuthorization(), params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        return resp;
    }


    public static GetTokensByCodeResponse2 tokenByInvalidCode(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret, String nonce) {

        final String state = CoreUtils.secureRandomString();
        codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce);

        final String code = CoreUtils.secureRandomString();

        String testOxdId = site.getOxdId();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(testOxdId);
        params.setCode(code);
        params.setState(state);

        GetTokensByCodeResponse2 resp = null;

        try {
            resp = client.getTokenByCode(Tester.getAuthorization(), params);
            assertNotNull(resp);
            notEmpty(resp.getAccessToken());
            notEmpty(resp.getIdToken());
            notEmpty(resp.getRefreshToken());

        } catch (BadRequestException ex) {
            assertEquals(ex.getMessage(), "HTTP 400 Bad Request");
        }

        return resp;
    }

    public static String codeRequest(ClientInterface client, String siteId, String userId, String userSecret, String state, String nonce) {
        GetAuthorizationCodeParams params = new GetAuthorizationCodeParams();
        params.setOxdId(siteId);
        params.setUsername(userId);
        params.setPassword(userSecret);
        params.setState(state);
        params.setNonce(nonce);

        return client.getAuthorizationCode(Tester.getAuthorization(), params).getCode();
    }

}

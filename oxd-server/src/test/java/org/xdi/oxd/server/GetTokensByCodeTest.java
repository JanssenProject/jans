package org.xdi.oxd.server;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.GetAccessTokenByRefreshTokenParams;
import org.xdi.oxd.common.params.GetAuthorizationCodeParams;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.response.GetClientTokenResponse;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.server.service.ConfigurationService;
import org.xdi.oxd.server.service.StateService;

import javax.ws.rs.BadRequestException;
import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2015
 */

public class GetTokensByCodeTest {


    @Parameters({"host", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void whenValidCodeIsUsed_shouldGetTokenInResponse(String host, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        GetTokensByCodeResponse tokensResponse = tokenByCode(client, site, userId, userSecret, CoreUtils.secureRandomString());
        refreshToken(tokensResponse, client, site.getOxdId());
    }



    @Parameters({"host", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void whenInvalidCodeIsUsed_shouldGet400BadRequest(String host, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        tokenByInvalidCode(client, site, userId, userSecret, CoreUtils.secureRandomString());
    }




    public static GetClientTokenResponse refreshToken(GetTokensByCodeResponse resp, ClientInterface client, String oxdId) {
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

    public static GetTokensByCodeResponse tokenByCode(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret, String nonce) {

        final String state = CoreUtils.secureRandomString();

        String code = codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce);

        notEmpty(code);

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(code);
        params.setState(state);

        final GetTokensByCodeResponse resp = client.getTokenByCode(Tester.getAuthorization(), params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        notEmpty(resp.getRefreshToken());
        return resp;
    }


    public static GetTokensByCodeResponse tokenByInvalidCode(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret, String nonce) {

        final String state = CoreUtils.secureRandomString();
        codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce);

        final String code = CoreUtils.secureRandomString();

        String testOxdId = site.getOxdId();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(testOxdId);
        params.setCode(code);
        params.setState(state);

        GetTokensByCodeResponse resp = null;

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

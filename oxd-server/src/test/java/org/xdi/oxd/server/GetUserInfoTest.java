package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.params.GetUserInfoParams;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.GetUserInfoResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2015
 */

public class GetUserInfoTest {

    @Parameters({"host", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, String opHost, String redirectUrl, String userId, String userSecret) {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        final GetTokensByCodeResponse tokens = requestTokens(client, site, userId, userSecret);

        GetUserInfoParams params = new GetUserInfoParams();
        params.setOxdId(site.getOxdId());
        params.setAccessToken(tokens.getAccessToken());

        final GetUserInfoResponse resp = client.getUserInfo(Tester.getAuthorization(), params).dataAsResponse(GetUserInfoResponse.class);
        assertNotNull(resp);
        notEmpty(resp.getClaims().get("sub"));
    }

    private GetTokensByCodeResponse requestTokens(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret) {

        final String state = CoreUtils.secureRandomString();
        final String nonce = CoreUtils.secureRandomString();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(GetTokensByCodeTest.codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce));
        params.setState(state);

        final GetTokensByCodeResponse resp = client.getTokenByCode(Tester.getAuthorization(), params).dataAsResponse(GetTokensByCodeResponse.class);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        return resp;
    }
}

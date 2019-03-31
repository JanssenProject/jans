package org.gluu.oxd.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.jettison.json.JSONException;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.params.GetTokensByCodeParams;
import org.gluu.oxd.common.params.GetUserInfoParams;
import org.gluu.oxd.common.response.GetTokensByCodeResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;

import static junit.framework.Assert.assertNotNull;
import static org.gluu.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2015
 */

public class GetUserInfoTest {

    @Parameters({"host", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, String opHost, String redirectUrl, String userId, String userSecret) throws JSONException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        final GetTokensByCodeResponse tokens = requestTokens(client, site, userId, userSecret);

        GetUserInfoParams params = new GetUserInfoParams();
        params.setOxdId(site.getOxdId());
        params.setAccessToken(tokens.getAccessToken());

        final JsonNode resp = client.getUserInfo(Tester.getAuthorization(), params);
        assertNotNull(resp);
        assertNotNull(resp.get("sub"));
    }

    private GetTokensByCodeResponse requestTokens(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret) {

        final String state = CoreUtils.secureRandomString();
        final String nonce = CoreUtils.secureRandomString();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(site.getOxdId());
        params.setCode(GetTokensByCodeTest.codeRequest(client, site.getOxdId(), userId, userSecret, state, nonce));
        params.setState(state);

        final GetTokensByCodeResponse resp = client.getTokenByCode(Tester.getAuthorization(), params);
        assertNotNull(resp);
        notEmpty(resp.getAccessToken());
        notEmpty(resp.getIdToken());
        return resp;
    }
}

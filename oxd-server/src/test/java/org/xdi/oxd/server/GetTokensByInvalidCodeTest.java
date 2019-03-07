package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.GetTokensByCodeParams;
import org.xdi.oxd.common.response.GetTokensByCodeResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import javax.ws.rs.BadRequestException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GetTokensByInvalidCodeTest {


    @Parameters({"host", "opHost", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, String opHost, String redirectUrl, String userId, String userSecret) throws IOException {
        ClientInterface client = Tester.newClient(host);
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        whenInvalidCodeIsUsed_shouldGet400BadRequest(client, site, userId, userSecret, CoreUtils.secureRandomString());
    }


    public static GetTokensByCodeResponse whenInvalidCodeIsUsed_shouldGet400BadRequest(ClientInterface client, RegisterSiteResponse site, String userId, String userSecret, String nonce) {

        final String state = CoreUtils.secureRandomString();
        final String code = CoreUtils.secureRandomString();

        String testOxdId = site.getOxdId();

        final GetTokensByCodeParams params = new GetTokensByCodeParams();
        params.setOxdId(testOxdId);
        params.setCode(code);
        params.setState(state);

        GetTokensByCodeResponse resp = null;

        try {
            resp = client.getTokenByCode(Tester.getAuthorization(), params);
            assertTrue(false);

        } catch (BadRequestException ex) {
            assertEquals(ex.getMessage(), "HTTP 400 Bad Request");
        }

        return resp;
    }
}

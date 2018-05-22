package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.params.GetLogoutUrlParams;
import org.xdi.oxd.common.response.LogoutResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Dummy test because we can't check real session management which is handled via browser cookies.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/11/2015
 */

public class GetLogoutUrlTest {

    @Parameters({"host", "opHost", "redirectUrl", "postLogoutRedirectUrl"})
    @Test
    public void test(String host, String opHost, String redirectUrl, String postLogoutRedirectUrl) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl, postLogoutRedirectUrl, "");

        final GetLogoutUrlParams params = new GetLogoutUrlParams();
        params.setOxdId(site.getOxdId());
        params.setIdTokenHint("dummy_token");
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setState(UUID.randomUUID().toString());
        params.setSessionState(UUID.randomUUID().toString()); // here must be real session instead of dummy UUID

        final LogoutResponse resp = client.getLogoutUri(Tester.getAuthorization(), params).dataAsResponse(LogoutResponse.class);
        assertNotNull(resp);
        assertTrue(resp.getUri().contains(URLEncoder.encode(postLogoutRedirectUrl, "UTF-8")));
    }
}

package org.gluu.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.GetLogoutUrlParams;
import org.gluu.oxd.common.response.GetLogoutUriResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Dummy test because we can't check real session management which is handled via browser cookies.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/11/2015
 */

public class GetLogoutUrlTest {

    @Parameters({"host", "opHost", "redirectUrls", "postLogoutRedirectUrl"})
    @Test
    public void test(String host, String opHost, String redirectUrls, String postLogoutRedirectUrl) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls, postLogoutRedirectUrl, "", false);

        final GetLogoutUrlParams params = new GetLogoutUrlParams();
        params.setOxdId(site.getOxdId());
        params.setIdTokenHint("dummy_token");
        params.setPostLogoutRedirectUri(postLogoutRedirectUrl);
        params.setState(UUID.randomUUID().toString());
        params.setSessionState(UUID.randomUUID().toString()); // here must be real session instead of dummy UUID

        final GetLogoutUriResponse resp = client.getLogoutUri(Tester.getAuthorization(site), null, params);
        assertNotNull(resp);
        assertTrue(resp.getUri().contains(URLEncoder.encode(postLogoutRedirectUrl, "UTF-8")));
    }
}

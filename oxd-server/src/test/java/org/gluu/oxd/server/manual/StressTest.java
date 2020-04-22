package org.gluu.oxd.server.manual;

import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.GetAuthorizationUrlParams;
import org.gluu.oxd.common.response.GetAuthorizationUrlResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.server.RegisterSiteTest;
import org.gluu.oxd.server.Tester;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.gluu.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/08/2016
 */

public class StressTest {

    @Parameters({"host", "redirectUrl", "opHost"})
    @Test(invocationCount = 10, threadPoolSize = 10, enabled = true)
    public void test(String host, String redirectUrl, String opHost) {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        final GetAuthorizationUrlParams params = new GetAuthorizationUrlParams();
        params.setOxdId(site.getOxdId());

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(site), null, params);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        assertTrue(resp.getAuthorizationUrl().contains("acr_values"));
    }
}

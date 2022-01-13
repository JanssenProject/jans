package io.jans.ca.server.manual;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetAuthorizationUrlParams;
import io.jans.ca.common.response.GetAuthorizationUrlResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.RegisterSiteTest;
import io.jans.ca.server.Tester;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static io.jans.ca.server.TestUtils.notEmpty;

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
        params.setRpId(site.getRpId());

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(site), null, params);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        assertTrue(resp.getAuthorizationUrl().contains("acr_values"));
    }
}

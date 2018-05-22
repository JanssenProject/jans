package org.xdi.oxd.server.manual;

import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;
import org.xdi.oxd.common.response.GetAuthorizationUrlResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.server.RegisterSiteTest;
import org.xdi.oxd.server.Tester;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/08/2016
 */

public class StressTest {

    @Parameters({"host", "redirectUrl", "opHost"})
    @Test(invocationCount = 10, threadPoolSize = 10, enabled = true)
    public void test(String host, String redirectUrl, String opHost) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        final GetAuthorizationUrlParams params = new GetAuthorizationUrlParams();
        params.setOxdId(site.getOxdId());

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(), params).dataAsResponse(GetAuthorizationUrlResponse.class);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        Assert.assertTrue(resp.getAuthorizationUrl().contains("acr_values"));
    }
}

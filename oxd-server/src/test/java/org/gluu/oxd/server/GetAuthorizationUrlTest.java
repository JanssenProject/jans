package org.gluu.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.GetAuthorizationUrlParams;
import org.gluu.oxd.common.response.GetAuthorizationUrlResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import com.google.common.collect.Lists;

import java.io.IOException;
import static org.testng.AssertJUnit.assertNotNull;
import static org.gluu.oxd.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class GetAuthorizationUrlTest {
    @Parameters({"host", "redirectUrl", "opHost"})
    @Test
    public void test(String host, String redirectUrl, String opHost) {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(site.getOxdId());

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(), commandParams);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
    }

    @Parameters({"host", "redirectUrl", "opHost", "redirectUrls", "postLogoutRedirectUrl", "logoutUrl", "paramRedirectUrl"})
    @Test
    public void testWithParameterAuthorizationUrl(String host, String redirectUrl, String opHost, String redirectUrls, String postLogoutRedirectUrl, String logoutUrl, String paramRedirectUrl) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl, postLogoutRedirectUrl, logoutUrl,
                StringUtils.isNotBlank(redirectUrls) ? Lists.newArrayList(redirectUrls.split(" ")) : null);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setAuthorizationRedirectUri(paramRedirectUrl);

        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(), commandParams);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
        assertTrue(resp.getAuthorizationUrl().contains(paramRedirectUrl));

    }
}

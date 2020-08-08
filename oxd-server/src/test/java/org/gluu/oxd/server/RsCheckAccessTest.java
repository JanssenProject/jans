package org.gluu.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.RsCheckAccessParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RsCheckAccessResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/07/2017
 */

public class RsCheckAccessTest {

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void withoutScopes_shouldPass(String host, String opHost, String redirectUrls, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        checkAccess(client, site, null);
    }

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void withCorrectScopes_shouldPass(String host, String opHost, String redirectUrls, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        checkAccess(client, site, org.testng.collections.Lists.newArrayList("http://photoz.example.com/dev/actions/all", "http://photoz.example.com/dev/actions/view"));
    }

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void withIncorrectScopes_shouldThrowException(String host, String opHost, String redirectUrls, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        checkAccessWithIncorrectScopes(client, site, org.testng.collections.Lists.newArrayList("/dev/actions/all", "/dev/actions/view"));
    }

    public static RsCheckAccessResponse checkAccess(ClientInterface client, RegisterSiteResponse site, List<String> scopeList) {
        final RsCheckAccessParams params = new RsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");
        params.setScopes(scopeList);

        final RsCheckAccessResponse response = client.umaRsCheckAccess(Tester.getAuthorization(site), null, params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getAccess()));
        return response;
    }

    public static void checkAccessWithIncorrectScopes(ClientInterface client, RegisterSiteResponse site, List<String> scopeList) {
        final RsCheckAccessParams params = new RsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");
        params.setScopes(scopeList);
        try {
            client.umaRsCheckAccess(Tester.getAuthorization(site), null, params);
            assertTrue(false);

        } catch (Exception e) {
            //test-case passed
        }
    }
}

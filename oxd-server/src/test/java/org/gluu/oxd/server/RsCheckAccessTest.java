package org.gluu.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.RsCheckAccessParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RsCheckAccessResponse;

import java.io.IOException;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/07/2017
 */

public class RsCheckAccessTest {

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void test(String host, String opHost, String redirectUrls, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        checkAccess(client, site);
    }

    public static RsCheckAccessResponse checkAccess(ClientInterface client, RegisterSiteResponse site) {
        final RsCheckAccessParams params = new RsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");

        final RsCheckAccessResponse response = client.umaRsCheckAccess(Tester.getAuthorization(), params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getAccess()));
        return response;
    }
}

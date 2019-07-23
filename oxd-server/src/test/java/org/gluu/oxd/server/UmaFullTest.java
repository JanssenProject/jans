package org.gluu.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.params.RpGetRptParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RpGetRptResponse;
import org.gluu.oxd.common.response.RsCheckAccessResponse;
import org.gluu.oxd.rs.protect.RsResourceList;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.UUID;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/06/2016
 */

public class UmaFullTest {

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void test(String host, String redirectUrls, String opHost, String rsProtect) throws Exception {

        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final RpGetRptParams params = new RpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());

        final RpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(), params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getRpt()));
        assertTrue(StringUtils.isNotBlank(response.getPct()));
    }

    public static RsResourceList resourceList(String rsProtect) throws IOException {
        rsProtect = StringUtils.replace(rsProtect, "'", "\"");
        return Jackson2.createJsonMapper().readValue(rsProtect, RsResourceList.class);
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void testWithInvalidTicket(String host, String redirectUrls, String opHost, String rsProtect) throws Exception {

        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RpGetRptParams params = new RpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(UUID.randomUUID().toString());

        try {
            client.umaRpGetRpt(Tester.getAuthorization(), params);
        } catch (BadRequestException ex) {
            return;
        }
        throw new AssertionError();
    }
}

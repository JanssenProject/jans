package io.jans.ca.mock;

import org.apache.commons.lang.StringUtils;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.common.response.RsCheckAccessResponse;
import io.jans.ca.rs.protect.RsResourceList;
import io.jans.ca.server.RegisterSiteTest;
import io.jans.ca.server.RsCheckAccessTest;
import io.jans.ca.server.RsProtectTest;
import io.jans.ca.server.Tester;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/06/2016
 */

public class UmaFullTest {

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void mockTest(String host, String redirectUrls, String opHost, String rsProtect) throws Exception {

        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final RpGetRptParams params = new RpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());

        final RpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(), null, params);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getRpt()));
        assertTrue(StringUtils.isNotBlank(response.getPct()));
    }

    public static RsResourceList resourceList(String rsProtect) throws IOException {
        rsProtect = StringUtils.replace(rsProtect, "'", "\"");
        return Jackson2.createJsonMapper().readValue(rsProtect, RsResourceList.class);
    }

}

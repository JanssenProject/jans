package io.swagger.client.api;

import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.UmaRpGetRptParams;
import io.swagger.client.model.UmaRpGetRptResponse;
import io.swagger.client.model.UmaRsCheckAccessResponse;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static io.swagger.client.api.Tester.api;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 *
 * @version 11/03/2018
 */

public class RpGetRptTest {

    @Parameters({"opHost", "redirectUrls", "rsProtect"})
    @Test
    public void test(String opHost, String redirectUrls, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final UmaRpGetRptResponse response = requestRpt(client, site, rsProtect);

        assertNotNull(response);

    }


    @Parameters({"opHost", "redirectUrls", "rsProtect"})
    @Test
    public void testWithSameRpt(String opHost, String redirectUrls, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final UmaRpGetRptResponse firstResponse = requestRpt(client, site, rsProtect);

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setRpt(firstResponse.getAccessToken());

        final UmaRpGetRptResponse secondResponse = client.umaRpGetRpt(Tester.getAuthorization(), params);

        assertNotNull(secondResponse);
        assertEquals(secondResponse.getAccessToken(), firstResponse.getAccessToken());
        assertFalse(firstResponse.isUpdated());
        assertTrue(secondResponse.isUpdated());

    }


    private static UmaRpGetRptResponse requestRpt(DevelopersApi client, RegisterSiteResponse site, String rsProtect) throws Exception {
        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());

        final UmaRpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(), params);

        assertNotNull(response);
        assertTrue(isNotBlank(response.getAccessToken()));
        assertTrue(isNotBlank(response.getPct()));
        return response;
    }
}

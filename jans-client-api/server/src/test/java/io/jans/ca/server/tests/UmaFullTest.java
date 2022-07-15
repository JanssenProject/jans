package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.RpGetRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.common.response.RsCheckAccessResponse;
import io.jans.ca.rs.protect.RsResourceList;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/06/2016
 */

public class UmaFullTest extends BaseTest {
    @ArquillianResource
    private URI url;

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void test(String host, String redirectUrls, String opHost, String rsProtect) throws Exception {

        ClientInterface client = getClientInterface(url);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final RpGetRptParams params = new RpGetRptParams();
        params.setRpId(site.getRpId());
        params.setTicket(checkAccess.getTicket());

        final RpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(client.getApitargetURL(), site), params.getRpId(), params);

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

        ClientInterface client = getClientInterface(url);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RpGetRptParams params = new RpGetRptParams();
        params.setRpId(site.getRpId());
        params.setTicket(UUID.randomUUID().toString());

        RpGetRptResponse r = client.umaRpGetRpt(Tester.getAuthorization(client.getApitargetURL(), site), params.getRpId(), params);
        assertNotNull(r);
        assertEquals(r.getError(), "invalid_ticket");
    }
}

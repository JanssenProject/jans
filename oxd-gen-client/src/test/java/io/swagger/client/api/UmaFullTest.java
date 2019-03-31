package io.swagger.client.api;

import io.swagger.client.ApiException;
import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.RsResource;
import io.swagger.client.model.UmaRpGetRptParams;
import io.swagger.client.model.UmaRpGetRptResponse;
import io.swagger.client.model.UmaRsCheckAccessResponse;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.rs.protect.Jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.swagger.client.api.Tester.api;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.replace;
import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 *
 * @version 11/08/2018
 */

public class UmaFullTest {

    @Parameters({"redirectUrl", "opHost", "rsProtect"})
    @Test
    public void test(String redirectUrl, String opHost, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());

        final UmaRpGetRptResponse response = client.umaRpGetRpt(Tester.getAuthorization(), params);

        assertNotNull(response);

        assertTrue(isNotBlank(response.getAccessToken()));
        assertTrue(isNotBlank(response.getPct()));
    }

    @Parameters({"redirectUrl", "opHost", "rsProtect"})
    @Test
    public void testWithInvalidTicket(String redirectUrl, String opHost, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(UUID.randomUUID().toString());

        try {
            client.umaRpGetRpt(Tester.getAuthorization(), params);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);  // BAD REQUEST
        }

    }


    @Parameters({"redirectUrl", "opHost", "rsProtect"})
    @Test
    public void testWithClaimTokenButNoTokenFormat(String redirectUrl, String opHost, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setClaimToken(Tester.getAuthorization(site));

        try {
            client.umaRpGetRpt(Tester.getAuthorization(), params);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);
        }

    }

    public static List<RsResource> resourceList(String rsProtect) throws IOException {
        rsProtect = replace(rsProtect, "'", "\"");

        final ObjectMapper jsonMapper = Jackson.createJsonMapper();
        final JsonNode resourcesNode = jsonMapper.readTree(rsProtect).get(("resources"));

        if (resourcesNode != null)
            return jsonMapper.readValue(resourcesNode, ArrayList.class);

        throw new IllegalArgumentException("Could not find test data for protected resources");

    }

}

package io.swagger.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.client.ApiException;
import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.RsResource;
import io.swagger.client.model.UmaRpGetRptParams;
import io.swagger.client.model.UmaRpGetRptResponse;
import io.swagger.client.model.UmaRsCheckAccessResponse;
import org.gluu.oxd.common.Jackson2;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

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

    @Parameters({"redirectUrls", "opHost", "rsProtect"})
    @Test
    public void test(String redirectUrls, String opHost, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());

        final UmaRpGetRptResponse response = client.umaRpGetRpt(params, Tester.getAuthorization(site), null);

        assertNotNull(response);

        assertTrue(isNotBlank(response.getAccessToken()));
        assertTrue(isNotBlank(response.getPct()));
    }

    @Parameters({"redirectUrls", "opHost", "rsProtect"})
    @Test
    public void testWithInvalidTicket(String redirectUrls, String opHost, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(UUID.randomUUID().toString());

        try {
            client.umaRpGetRpt(params, Tester.getAuthorization(site), null);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);  // BAD REQUEST
        }

    }


    @Parameters({"redirectUrls", "opHost", "rsProtect"})
    @Test
    public void testWithClaimTokenButNoTokenFormat(String redirectUrls, String opHost, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final UmaRpGetRptParams params = new UmaRpGetRptParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setClaimToken(Tester.getAuthorization(site));

        try {
            client.umaRpGetRpt(params, Tester.getAuthorization(site), null);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);
        }

    }

    public static List<RsResource> resourceList(String rsProtect) throws IOException {
        rsProtect = replace(rsProtect, "'", "\"");

        final ObjectMapper jsonMapper = Jackson2.createJsonMapper();
        final JsonNode resourcesNode = jsonMapper.readTree(rsProtect).get(("resources"));

        if (resourcesNode != null)
            return jsonMapper.treeToValue(resourcesNode, ArrayList.class);

        throw new IllegalArgumentException("Could not find test data for protected resources");

    }

}

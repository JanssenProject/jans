package io.swagger.client.api;

import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.UmaRpGetClaimsGatheringUrlParams;
import io.swagger.client.model.UmaRpGetClaimsGatheringUrlResponse;
import io.swagger.client.model.UmaRsCheckAccessResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.common.CoreUtils;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author yuriyz
 * @author Shoeb
 *
 * @version 11/12/2018
 */
public class UmaGetClaimsGatheringUrlTest {

    @Parameters({"opHost", "redirectUrls", "rsProtect", "paramRedirectUrl"})
    @Test
    public void test(String opHost, String redirectUrls, String rsProtect, String paramRedirectUrl) throws Exception {

        final DevelopersApi client = Tester.api();
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final UmaRpGetClaimsGatheringUrlParams params = new UmaRpGetClaimsGatheringUrlParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setClaimsRedirectUri(paramRedirectUrl);

        final UmaRpGetClaimsGatheringUrlResponse response = client.umaRpGetClaimsGatheringUrl(Tester.getAuthorization(), params);

        final Map<String, String> parameters = CoreUtils.splitQuery(response.getUrl());

        assertTrue(isNotBlank(parameters.get("client_id")));
        assertTrue(isNotBlank(parameters.get("ticket")));
        assertTrue(isNotBlank(parameters.get("state")));
        assertTrue(isNotBlank(response.getState()));
        assertEquals(paramRedirectUrl, parameters.get("claims_redirect_uri"));
    }

    @Parameters({"opHost", "redirectUrls", "rsProtect", "state", "paramRedirectUrl"})
    @Test
    public void testWithCustomStateParameter(String opHost, String redirectUrls, String rsProtect, String state, String paramRedirectUrl) throws Exception {

        final DevelopersApi client = Tester.api();
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final UmaRpGetClaimsGatheringUrlParams params = new UmaRpGetClaimsGatheringUrlParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setClaimsRedirectUri(paramRedirectUrl);
        params.setState(state);

        final UmaRpGetClaimsGatheringUrlResponse response = client.umaRpGetClaimsGatheringUrl(Tester.getAuthorization(), params);

        final Map<String, String> parameters = CoreUtils.splitQuery(response.getUrl());

        assertTrue(isNotBlank(parameters.get("client_id")));
        assertTrue(isNotBlank(parameters.get("ticket")));
        assertTrue(isNotBlank(parameters.get("state")));
        assertTrue(isNotBlank(response.getState()));
        assertEquals(response.getState(), state);
        assertEquals(paramRedirectUrl, parameters.get("claims_redirect_uri"));
    }
}

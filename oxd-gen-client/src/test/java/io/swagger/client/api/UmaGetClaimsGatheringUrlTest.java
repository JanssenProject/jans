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

    @Parameters({"opHost", "redirectUrl", "rsProtect"})
    @Test
    public void test(String opHost, String redirectUrl, String rsProtect) throws Exception {

        final DevelopersApi client = Tester.api();
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        final UmaRsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

        final UmaRpGetClaimsGatheringUrlParams params = new UmaRpGetClaimsGatheringUrlParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setClaimsRedirectUri(redirectUrl);

        final UmaRpGetClaimsGatheringUrlResponse response = client.umaRpGetClaimsGatheringUrl(Tester.getAuthorization(), params);

        final Map<String, String> parameters = CoreUtils.splitQuery(response.getUrl());

        assertTrue(isNotBlank(parameters.get("client_id")));
        assertTrue(isNotBlank(parameters.get("ticket")));
        assertTrue(isNotBlank(parameters.get("state")));
        assertTrue(isNotBlank(response.getState()));
        assertEquals(redirectUrl, parameters.get("claims_redirect_uri"));
    }
}

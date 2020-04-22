package org.gluu.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.params.RpGetClaimsGatheringUrlParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RpGetClaimsGatheringUrlResponse;
import org.gluu.oxd.common.response.RsCheckAccessResponse;
import org.testng.collections.Maps;

import java.io.IOException;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author yuriyz
 */
public class UmaGetClaimsGatheringUrlTest {

    @Parameters({"host", "opHost", "paramRedirectUrl", "rsProtect"})
    @Test
    public void test(String host, String opHost, String paramRedirectUrl, String rsProtect) throws IOException {

        ClientInterface client = Tester.newClient(host);
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, paramRedirectUrl);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final RpGetClaimsGatheringUrlParams params = new RpGetClaimsGatheringUrlParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setClaimsRedirectUri(paramRedirectUrl);

        final RpGetClaimsGatheringUrlResponse response = client.umaRpGetClaimsGatheringUrl(Tester.getAuthorization(site), null, params);

        Map<String, String> parameters = CoreUtils.splitQuery(response.getUrl());

        assertTrue(StringUtils.isNotBlank(parameters.get("client_id")));
        assertTrue(StringUtils.isNotBlank(parameters.get("ticket")));
        assertTrue(StringUtils.isNotBlank(parameters.get("state")));
        assertTrue(StringUtils.isNotBlank(response.getState()));
        assertEquals(paramRedirectUrl, parameters.get("claims_redirect_uri"));
    }

    @Parameters({"host", "opHost", "paramRedirectUrl", "rsProtect"})
    @Test
    public void test_withCustomParameter(String host, String opHost, String paramRedirectUrl, String rsProtect) throws IOException {

        ClientInterface client = Tester.newClient(host);
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, paramRedirectUrl);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final RpGetClaimsGatheringUrlParams params = new RpGetClaimsGatheringUrlParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setClaimsRedirectUri(paramRedirectUrl);

        Map<String, String> customParameterMap = Maps.newHashMap();
        customParameterMap.put("param1", "value1");
        customParameterMap.put("param2", "value2");
        params.setCustomParameters(customParameterMap);

        final RpGetClaimsGatheringUrlResponse response = client.umaRpGetClaimsGatheringUrl(Tester.getAuthorization(site), null, params);

        Map<String, String> parameters = CoreUtils.splitQuery(response.getUrl());

        assertTrue(StringUtils.isNotBlank(parameters.get("client_id")));
        assertTrue(StringUtils.isNotBlank(parameters.get("ticket")));
        assertTrue(StringUtils.isNotBlank(parameters.get("state")));
        assertTrue(StringUtils.isNotBlank(response.getState()));
        assertTrue(StringUtils.isNotBlank(parameters.get("param1")));
        assertTrue(StringUtils.isNotBlank(parameters.get("param2")));
        assertEquals(paramRedirectUrl, parameters.get("claims_redirect_uri"));
    }

    @Parameters({"host", "opHost", "paramRedirectUrl", "rsProtect", "state"})
    @Test
    public void test_withState(String host, String opHost, String paramRedirectUrl, String rsProtect, String state) throws IOException {

        ClientInterface client = Tester.newClient(host);
        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, paramRedirectUrl);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

        final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site, null);

        final RpGetClaimsGatheringUrlParams params = new RpGetClaimsGatheringUrlParams();
        params.setOxdId(site.getOxdId());
        params.setTicket(checkAccess.getTicket());
        params.setClaimsRedirectUri(paramRedirectUrl);
        params.setState(state);

        final RpGetClaimsGatheringUrlResponse response = client.umaRpGetClaimsGatheringUrl(Tester.getAuthorization(site), null, params);

        Map<String, String> parameters = CoreUtils.splitQuery(response.getUrl());

        assertTrue(StringUtils.isNotBlank(parameters.get("client_id")));
        assertTrue(StringUtils.isNotBlank(parameters.get("ticket")));
        assertTrue(StringUtils.isNotBlank(parameters.get("state")));
        assertTrue(StringUtils.isNotBlank(response.getState()));
        assertEquals(paramRedirectUrl, parameters.get("claims_redirect_uri"));
        assertEquals(response.getState(), state);
    }
}

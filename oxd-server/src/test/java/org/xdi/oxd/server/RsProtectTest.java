package org.xdi.oxd.server;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.ErrorResponse;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.oxd.common.response.RsProtectResponse;
import org.xdi.oxd.rs.protect.RsResource;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 10/06/2016
 */

public class RsProtectTest {

    @Parameters({"host", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void protect(String host, String redirectUrl, String opHost, String rsProtect) throws IOException {

        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());
        RsCheckAccessTest.checkAccess(client, site);
    }

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void overwriteFalse(String host, String redirectUrl, String opHost, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        List<RsResource> resources = UmaFullTest.resourceList(rsProtect).getResources();
        protectResources(client, site, resources);

        final RsProtectParams params = new RsProtectParams();
        params.setOxdId(site.getOxdId());
        params.setResources(resources);

        ErrorResponse errorResponse = client.umaRsProtect(Tester.getAuthorization(), params).dataAsResponse(ErrorResponse.class);
        assertNotNull(errorResponse);
        assertEquals(errorResponse.getError(), "uma_protection_exists");
    }

    @Parameters({"host", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void overwriteTrue(String host, String redirectUrl, String opHost, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        List<RsResource> resources = UmaFullTest.resourceList(rsProtect).getResources();
        protectResources(client, site, resources);

        final RsProtectParams params = new RsProtectParams();
        params.setOxdId(site.getOxdId());
        params.setResources(resources);
        params.setOverwrite(true); // force overwrite

        RsProtectResponse response = client.umaRsProtect(Tester.getAuthorization(), params).dataAsResponse(RsProtectResponse.class);
        assertNotNull(response);
    }

    @Parameters({"host", "redirectUrl", "opHost", "rsProtectScopeExpression"})
    @Test
    public void protectWithScopeExpression(String host, String redirectUrl, String opHost, String rsProtectScopeExpression) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpression).getResources());
        RsCheckAccessTest.checkAccess(client, site);
    }

    @Parameters({"host", "redirectUrl", "opHost", "rsProtectScopeExpressionSecond"})
    @Test
    public void protectWithScopeExpressionSeconds(String host, String redirectUrl, String opHost, String rsProtectScopeExpressionSecond) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpressionSecond).getResources());

        final RsCheckAccessParams params = new RsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/GetAll");
        params.setRpt("");

        final RsCheckAccessResponse response = client.umaRsCheckAccess(Tester.getAuthorization(), params).dataAsResponse(RsCheckAccessResponse.class);

        Assert.assertNotNull(response);
        Assert.assertTrue(StringUtils.isNotBlank(response.getAccess()));
    }

    public static RsProtectResponse protectResources(ClientInterface client, RegisterSiteResponse site, List<RsResource> resources) {
        final RsProtectParams params = new RsProtectParams();
        params.setOxdId(site.getOxdId());
        params.setResources(resources);

        final RsProtectResponse resp = client.umaRsProtect(Tester.getAuthorization(), params).dataAsResponse(RsProtectResponse.class);
        assertNotNull(resp);
        return resp;
    }
}

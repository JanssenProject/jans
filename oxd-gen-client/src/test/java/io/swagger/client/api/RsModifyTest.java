package io.swagger.client.api;

import io.swagger.client.model.*;
import org.gluu.oxd.common.Jackson2;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

import static io.swagger.client.api.Tester.api;
import static io.swagger.client.api.Tester.getAuthorization;
import static org.testng.Assert.assertNotNull;

public class RsModifyTest {
    @Parameters({"redirectUrls", "opHost", "rsProtect"})
    @Test
    public void protect(String redirectUrls, String opHost, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtect));
        RsCheckAccessTest.checkAccess(client, site);
        modifyResourcesWithScopes(client, site, UmaFullTest.resourceList(rsProtect));

    }

    @Parameters({"redirectUrls", "opHost", "rsProtectScopeExpression", "correctScopeExpression"})
    @Test
    public void protectWithScopeExpression(String redirectUrls, String opHost, String rsProtectScopeExpression, String correctScopeExpression) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpression));
        RsCheckAccessTest.checkAccess(client, site);
        modifyResourcesWithScopeExpression(client, site, UmaFullTest.resourceList(rsProtectScopeExpression), correctScopeExpression);

    }

    public static UmaRsProtectResponse protectResources(DevelopersApi client, RegisterSiteResponse site, List<RsResource> resources) throws Exception {
        final UmaRsProtectParams params = new UmaRsProtectParams();
        params.setOxdId(site.getOxdId());
        params.setResources(resources);

        final UmaRsProtectResponse resp = client.umaRsProtect(getAuthorization(), params);
        assertNotNull(resp);
        return resp;
    }

    public static UmaRsProtectResponse modifyResourcesWithScopes(DevelopersApi client, RegisterSiteResponse site, List<RsResource> resources) throws Exception {
        final UmaRsModifyParams params = new UmaRsModifyParams();
        params.setOxdId(site.getOxdId());
        RsResource rsResource = Jackson2.createJsonMapper().convertValue(resources.get(0), RsResource.class);
        params.setHttpMethod(rsResource.getConditions().get(0).getHttpMethods().get(0));
        params.setPath(rsResource.getPath());
        params.setScopes(Lists.newArrayList("http://photoz.example.com/dev/actions/see"));

        final UmaRsProtectResponse resp = client.umaRsModify(getAuthorization(), params);
        assertNotNull(resp.getOxdId());
        return resp;
    }

    public static UmaRsProtectResponse modifyResourcesWithScopeExpression(DevelopersApi client, RegisterSiteResponse site, List<RsResource> resources, String correctScopeExpression) throws Exception {
        final UmaRsModifyParams params = new UmaRsModifyParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setScopeExpression(correctScopeExpression.replaceAll("'", "\""));

        final UmaRsProtectResponse resp = client.umaRsModify(getAuthorization(), params);
        assertNotNull(resp.getOxdId());
        return resp;
    }
}

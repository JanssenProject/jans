package io.jans.ca.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.RsModifyParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RsModifyResponse;
import io.jans.ca.rs.protect.RsResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

public class RsModifyTest {

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void protect(String host, String redirectUrls, String opHost, String rsProtect) throws IOException {

        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());
        RsCheckAccessTest.checkAccess(client, site, null);
        modifyResourcesWithScopes(client, site, UmaFullTest.resourceList(rsProtect).getResources());
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtectScopeExpression", "correctScopeExpression"})
    @Test
    public void protectWithScopeExpression(String host, String redirectUrls, String opHost, String rsProtectScopeExpression, String correctScopeExpression) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpression).getResources());
        RsCheckAccessTest.checkAccess(client, site, null);
        modifyResourcesWithScopeExpression(client, site, UmaFullTest.resourceList(rsProtectScopeExpression).getResources(), correctScopeExpression);
    }

    public static RsModifyResponse modifyResourcesWithScopes(ClientInterface client, RegisterSiteResponse site, List<RsResource> resources) {

        final RsModifyParams params2 = new RsModifyParams();
        params2.setRpId(site.getRpId());
        params2.setHttpMethod(resources.get(0).getConditions().get(0).getHttpMethods().get(0));
        params2.setPath(resources.get(0).getPath());
        params2.setScopes(Lists.newArrayList("http://photoz.example.com/dev/actions/see"));

        RsModifyResponse response = client.umaRsModify(Tester.getAuthorization(site), null, params2);
        assertNotNull(response.getRpId());
        return response;
    }

    public static RsModifyResponse modifyResourcesWithScopeExpression(ClientInterface client, RegisterSiteResponse site, List<RsResource> resources, String correctScopeExpression) throws JsonProcessingException {

        final RsModifyParams params2 = new RsModifyParams();
        params2.setRpId(site.getRpId());
        params2.setHttpMethod(resources.get(0).getConditions().get(0).getHttpMethods().get(0));
        params2.setPath(resources.get(0).getPath());
        params2.setScopeExpression(correctScopeExpression.replaceAll("'", "\""));

        RsModifyResponse response = client.umaRsModify(Tester.getAuthorization(site), null, params2);
        assertNotNull(response.getRpId());
        return response;
    }
}

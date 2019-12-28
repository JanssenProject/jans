package org.gluu.oxd.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.assertj.core.util.Lists;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.client.RsProtectParams2;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.common.params.RsModifyParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RsModifyResponse;
import org.gluu.oxd.common.response.RsProtectResponse;
import org.gluu.oxd.rs.protect.RsResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

public class RsModifyTest {

    @Parameters({"host", "redirectUrls", "opHost", "rsProtect"})
    @Test
    public void protect(String host, String redirectUrls, String opHost, String rsProtect) throws IOException {

        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());
        RsCheckAccessTest.checkAccess(client, site);
        modifyResourcesWithScopes(client, site, UmaFullTest.resourceList(rsProtect).getResources());
    }

    @Parameters({"host", "redirectUrls", "opHost", "rsProtectScopeExpression", "correctScopeExpression"})
    @Test
    public void protectWithScopeExpression(String host, String redirectUrls, String opHost, String rsProtectScopeExpression, String correctScopeExpression) throws IOException {
        ClientInterface client = Tester.newClient(host);

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpression).getResources());
        RsCheckAccessTest.checkAccess(client, site);
        modifyResourcesWithScopeExpression(client, site, UmaFullTest.resourceList(rsProtectScopeExpression).getResources(), correctScopeExpression);
    }

    public static RsProtectResponse protectResources(ClientInterface client, RegisterSiteResponse site, List<RsResource> resources) {
        final RsProtectParams2 params = new RsProtectParams2();
        params.setOxdId(site.getOxdId());
        try {
            params.setResources(Jackson2.createJsonMapper().readTree(Jackson2.asJsonSilently(resources)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        final RsProtectResponse resp = client.umaRsProtect(Tester.getAuthorization(), params);
        assertNotNull(resp);
        return resp;
    }

    public static RsModifyResponse modifyResourcesWithScopes(ClientInterface client, RegisterSiteResponse site, List<RsResource> resources) {

        final RsModifyParams params2 = new RsModifyParams();
        params2.setOxdId(site.getOxdId());
        params2.setHttpMethod(resources.get(0).getConditions().get(0).getHttpMethods().get(0));
        params2.setPath(resources.get(0).getPath());
        params2.setScopes(Lists.newArrayList("http://photoz.example.com/dev/actions/see"));

        RsModifyResponse response = client.umaRsModify(Tester.getAuthorization(), params2);
        assertNotNull(response.getOxdId());
        return response;
    }

    public static RsModifyResponse modifyResourcesWithScopeExpression(ClientInterface client, RegisterSiteResponse site, List<RsResource> resources, String correctScopeExpression) throws JsonProcessingException {

        final RsModifyParams params2 = new RsModifyParams();
        params2.setOxdId(site.getOxdId());
        params2.setHttpMethod(resources.get(0).getConditions().get(0).getHttpMethods().get(0));
        params2.setPath(resources.get(0).getPath());
        params2.setScopeExpression(correctScopeExpression.replaceAll("'", "\""));

        RsModifyResponse response = client.umaRsModify(Tester.getAuthorization(), params2);
        assertNotNull(response.getOxdId());
        return response;
    }
}

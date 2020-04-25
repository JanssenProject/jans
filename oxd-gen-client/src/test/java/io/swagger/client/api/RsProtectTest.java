package io.swagger.client.api;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.model.*;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;

import static io.swagger.client.api.Tester.api;
import static io.swagger.client.api.Tester.getAuthorization;
import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 * @version 11/08/2018
 */
public class RsProtectTest {

    @Parameters({"redirectUrls", "opHost", "rsProtect"})
    @Test
    public void protect(String redirectUrls, String opHost, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtect));
        RsCheckAccessTest.checkAccess(client, site, null);

    }

    @Parameters({"redirectUrls", "opHost", "rsProtectWithCreationExpiration"})
    @Test
    public void protect_withResourceCreationExpiration(String redirectUrls, String opHost, String rsProtectWithCreationExpiration) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectWithCreationExpiration));
        RsCheckAccessTest.checkAccess(client, site, null);

    }

    @Parameters({"redirectUrls", "opHost", "rsProtect"})
    @Test
    public void overwriteFalse(String redirectUrls, String opHost, String rsProtect) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        final List<RsResource> resources = UmaFullTest.resourceList(rsProtect);
        protectResources(client, site, resources);

        final UmaRsProtectParams params = new UmaRsProtectParams();
        params.setOxdId(site.getOxdId());

        params.setResources(resources);

        try {
            final ApiResponse<?> response = client.umaRsProtectWithHttpInfo(params, getAuthorization(site), null);
            assertEquals(response.getStatusCode(), 400);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);
        }

    }

    @Parameters({"redirectUrls", "opHost", "rsProtect"})
    @Test
    public void overwriteTrue(String redirectUrls, String opHost, String rsProtect) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        final List<RsResource> resources = UmaFullTest.resourceList(rsProtect);
        protectResources(client, site, resources);

        final UmaRsProtectParams params = new UmaRsProtectParams();
        params.setOxdId(site.getOxdId());
        params.setResources(resources);
        params.setOverwrite(true); // force overwrite

        final UmaRsProtectResponse response = client.umaRsProtect(params, getAuthorization(site), null);
        assertNotNull(response);
    }

    @Parameters({"redirectUrls", "opHost", "rsProtectScopeExpression"})
    @Test
    public void protectWithScopeExpression(String redirectUrls, String opHost, String rsProtectScopeExpression) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpression));

        RsCheckAccessTest.checkAccess(client, site, null);

    }

    @Parameters({"redirectUrls", "opHost", "rsProtectScopeExpressionSecond"})
    @Test
    public void protectWithScopeExpressionSeconds(String redirectUrls, String opHost, String rsProtectScopeExpressionSecond) throws Exception {
        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        protectResources(client, site, UmaFullTest.resourceList(rsProtectScopeExpressionSecond));

        final UmaRsCheckAccessParams params = new UmaRsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/GetAll");
        params.setRpt("");

        final UmaRsCheckAccessResponse response = client.umaRsCheckAccess(params, getAuthorization(site), null);

        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getAccess()));
    }

    public static UmaRsProtectResponse protectResources(DevelopersApi client, RegisterSiteResponse site, List<RsResource> resources) throws Exception {
        final UmaRsProtectParams params = new UmaRsProtectParams();
        params.setOxdId(site.getOxdId());
        params.setResources(resources);

        final UmaRsProtectResponse resp = client.umaRsProtect(params, getAuthorization(site), null);
        assertNotNull(resp);
        return resp;
    }
}

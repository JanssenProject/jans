package io.swagger.client.api;

import io.swagger.client.ApiResponse;
import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.UmaRsCheckAccessParams;
import io.swagger.client.model.UmaRsCheckAccessResponse;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

import static io.swagger.client.api.Tester.api;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertTrue;


/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 *
 * @version 11/02/2018
 */

public class RsCheckAccessTest {

    @Parameters({"opHost", "redirectUrls", "rsProtect"})
    @Test
    public void withoutScopes_shouldPass(String opHost, String redirectUrls, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        checkAccess(client, site, null);
    }

    @Parameters({"opHost", "redirectUrls", "rsProtect"})
    @Test
    public void withCorrectScopes_shouldPass(String opHost, String redirectUrls, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        checkAccess(client, site, Lists.newArrayList("http://photoz.example.com/dev/actions/all","http://photoz.example.com/dev/actions/view"));
    }

    @Parameters({"opHost", "redirectUrls", "rsProtect"})
    @Test
    public void withIncorrectScopes_shouldThrowException(String opHost, String redirectUrls, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        checkAccessWithIncorrectScopes(client, site, Lists.newArrayList("/dev/actions/all","/dev/actions/view"));
    }

    public static UmaRsCheckAccessResponse checkAccess(DevelopersApi client, RegisterSiteResponse site, List<String> scopeList) throws Exception {
        final UmaRsCheckAccessParams params = new UmaRsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");
        params.setScopes(scopeList);

        final ApiResponse<UmaRsCheckAccessResponse> apiResp = client.umaRsCheckAccessWithHttpInfo(params, Tester.getAuthorization(site), null);

        assertEquals(apiResp.getStatusCode(), 200)  ;  //fixme should be 401
        assertNotNull(apiResp.getData());
        assertTrue(StringUtils.isNotBlank(apiResp.getData().getAccess()));

        return apiResp.getData();
    }

    public static void checkAccessWithIncorrectScopes(DevelopersApi client, RegisterSiteResponse site, List<String> scopeList) throws Exception {
        final UmaRsCheckAccessParams params = new UmaRsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");
        params.setScopes(scopeList);

        try {
            client.umaRsCheckAccessWithHttpInfo(params, Tester.getAuthorization(site), null);
            assertTrue(false);

        } catch (Exception e) {
            //test-case passed
        }

    }
}

package io.swagger.client.api;

import io.swagger.client.ApiResponse;
import io.swagger.client.model.RegisterSiteResponse;
import io.swagger.client.model.UmaRsCheckAccessParams;
import io.swagger.client.model.UmaRsCheckAccessResponse;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static io.swagger.client.api.Tester.api;
import static org.testng.Assert.*;


/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 *
 * @version 11/02/2018
 */

public class RsCheckAccessTest {

    @Parameters({"opHost", "redirectUrls", "rsProtect"})
    @Test
    public void test(String opHost, String redirectUrls, String rsProtect) throws Exception {

        final DevelopersApi client = api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);

        RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect));

        checkAccess(client, site);
    }

    public static UmaRsCheckAccessResponse checkAccess(DevelopersApi client, RegisterSiteResponse site) throws Exception {
        final UmaRsCheckAccessParams params = new UmaRsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");

        final ApiResponse<UmaRsCheckAccessResponse> apiResp = client.umaRsCheckAccessWithHttpInfo(Tester.getAuthorization(), params);

        assertEquals(apiResp.getStatusCode(), 200)  ;  //fixme should be 401
        assertNotNull(apiResp.getData());
        assertTrue(StringUtils.isNotBlank(apiResp.getData().getAccess()));

        return apiResp.getData();
    }
}

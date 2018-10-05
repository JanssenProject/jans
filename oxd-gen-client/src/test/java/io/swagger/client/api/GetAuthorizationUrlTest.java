package io.swagger.client.api;

import io.swagger.client.model.GetAuthorizationUrlParams;
import io.swagger.client.model.GetAuthorizationUrlResponse;
import io.swagger.client.model.RegisterSiteResponseData;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class GetAuthorizationUrlTest {
    @Parameters({"redirectUrl", "opHost"})
    @Test
    public void test(String redirectUrl, String opHost) throws Exception {
        DevelopersApi api = Tester.api();

        final RegisterSiteResponseData site = RegisterSiteTest.registerSite(api, opHost, redirectUrl);
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(site.getOxdId());

        final GetAuthorizationUrlResponse resp = api.getAuthorizationUrl(Tester.getAuthorization(site), commandParams);
        assertNotNull(resp);
        Tester.notEmpty(resp.getData().getAuthorizationUrl());
    }
}

package io.swagger.client.api;

import io.swagger.client.model.RegisterSiteResponseData;
import io.swagger.client.model.RemoveSiteParams;
import io.swagger.client.model.UpdateSiteResponse;
import io.swagger.client.model.UpdateSiteResponseData;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


/**
 * @author Yuriy Zabrovarnyy
 */
public class RemoveSiteTest {

    @Test
    @Parameters({"opHost", "redirectUrl"})
    public void testRemoveSite(String opHost, String redirectUrl) throws Exception {
        final DevelopersApi api = Tester.api();
        RegisterSiteResponseData response = RegisterSiteTest.registerSite(api, opHost, redirectUrl);
        String oxdId = response.getOxdId();
        RemoveSiteParams params = new RemoveSiteParams();
        params.setOxdId(oxdId);
        UpdateSiteResponse removeSiteResp =
                api.removeSite(Tester.getAuthorization(), params);
        assertNotNull(removeSiteResp);
        final UpdateSiteResponseData responseData = removeSiteResp.getData();
        assertNotNull(responseData);
        assertTrue(StringUtils.isNotEmpty(responseData.getOxdId()));
    }
}

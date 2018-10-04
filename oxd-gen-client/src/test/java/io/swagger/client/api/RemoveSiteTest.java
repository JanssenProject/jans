package io.swagger.client.api;

import io.swagger.client.ApiException;
import io.swagger.client.model.RegisterSiteResponseData;
import io.swagger.client.model.RemoveSiteParams;
import io.swagger.client.model.RemoveSiteResponse;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.ErrorResponseCode;

import java.util.UUID;

import static org.junit.Assert.*;

public class RemoveSiteTest {

    @Test
    @Parameters({"opHost", "redirectUrl"})
    public void testRemoveSite(String opHost, String redirectUrl) throws Exception {
        final DevelopersApi api = Tester.api();
        RegisterSiteResponseData response = RegisterSiteTest.registerSite(api, opHost, redirectUrl);

        RemoveSiteParams params = new RemoveSiteParams();
        params.setOxdId(response.getOxdId());

        RemoveSiteResponse removeSiteResp = api.removeSite(Tester.getAuthorization(response), params);
        assertNotNull(removeSiteResp);
        assertNotNull(removeSiteResp.getData());
        assertTrue(StringUtils.isNotEmpty(removeSiteResp.getData().getOxdId()));
    }

    @Test
    public void testRemoveSiteWithInvalidOxdId() throws Exception {
        final String someRandomId = UUID.randomUUID().toString();
        final DevelopersApi api = Tester.api();

        RemoveSiteParams params = new RemoveSiteParams();
        params.setOxdId(someRandomId);
        try {
            api.removeSite(Tester.getAuthorization(), params);
        } catch (ApiException e) {
            assertEquals(e.getCode(), 400);
            assertEquals(Tester.asError(e).getError(), ErrorResponseCode.INVALID_OXD_ID.getCode());
            return;
        }
        throw new AssertionError("Expected 400 error but got successful result.");
    }
}

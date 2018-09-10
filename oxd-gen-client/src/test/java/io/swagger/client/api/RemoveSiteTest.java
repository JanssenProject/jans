package io.swagger.client.api;

import io.swagger.client.ApiResponse;
import io.swagger.client.model.RegisterSiteResponseData;
import io.swagger.client.model.RemoveSiteParams;
import io.swagger.client.model.RemoveSiteResponse;
import io.swagger.client.model.RemoveSiteResponseData;
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
        String oxdId = response.getOxdId();
        RemoveSiteParams params = new RemoveSiteParams();
        params.setOxdId(oxdId);
        RemoveSiteResponse removeSiteResp =
                api.removeSite(Tester.getAuthorization(), params);
        assertNotNull(removeSiteResp);
        final RemoveSiteResponseData responseData = removeSiteResp.getData();
        assertNotNull(responseData);
        assertTrue(StringUtils.isNotEmpty(responseData.getOxdId()));
    }

    @Test
    public void testRemoveSiteWithInvalidOxdId() throws Exception {
        final DevelopersApi api = Tester.api();
        RemoveSiteParams params = new RemoveSiteParams();
        final String someRandomId = UUID.randomUUID().toString();
        params.setOxdId(someRandomId);
        ApiResponse<RemoveSiteResponse> apiResponse =
                api.removeSiteWithHttpInfo(Tester.getAuthorization(), params);
        /*
        FIXME: Status code should be 404, and not 200.
         */
        assertEquals(apiResponse.getStatusCode(), 200);
        assertTrue("error".equalsIgnoreCase(apiResponse.getData().getStatus()));
        RemoveSiteResponse removeSiteResponse = apiResponse.getData();
        assertNotNull(removeSiteResponse);
        assertNotNull(removeSiteResponse.getData());
        assertNotNull(removeSiteResponse.getData().getError());
        final String error = removeSiteResponse.getData().getError();
        assertEquals(error, ErrorResponseCode.INVALID_OXD_ID.getCode());

    }


}

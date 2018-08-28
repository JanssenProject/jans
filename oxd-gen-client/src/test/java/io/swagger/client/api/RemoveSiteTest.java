package io.swagger.client.api;

import io.swagger.client.ApiResponse;
import io.swagger.client.model.RegisterSiteResponseData;
import io.swagger.client.model.RemoveSiteParams;
import io.swagger.client.model.UpdateSiteResponse;
import io.swagger.client.model.UpdateSiteResponseData;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RemoveSiteTest {

    @Test
    @Parameters({"opHost", "redirectUrl"})
    public void testRemoveSite(String opHost, String redirectUrl) throws Exception {
        final DevelopersApi api = Tester.api();
        RegisterSiteResponseData response = RegisterSiteTest.registerSite(api, opHost, redirectUrl);
        String oxdId = response.getOxdId();
        RemoveSiteParams params = new RemoveSiteParams();
        params.setOxdId(oxdId);
        ApiResponse<UpdateSiteResponse> apiResponse =
                api.removeSiteWithHttpInfo(Tester.getAuthorization(), params);
        assertTrue(apiResponse.getStatusCode() == 200);
        assertNotNull(apiResponse);
        assertNotNull(apiResponse.getData());
        /*
        Swagger hub doesn't generate RemoveSiteResponse for some reason, so we have to
        use UpdateSiteResponse.
         */
        UpdateSiteResponse removeSiteResponse = apiResponse.getData();
        final UpdateSiteResponseData responseData = removeSiteResponse.getData();
        assertNotNull(responseData);
        assertTrue(StringUtils.isNotEmpty(responseData.getOxdId()));
    }

    @Test
    public void testRemoveSiteWithInvalidOxdId() throws Exception {
        final DevelopersApi api = Tester.api();
        RemoveSiteParams params = new RemoveSiteParams();
        final String someRandomId = UUID.randomUUID().toString();
        params.setOxdId(someRandomId);
        ApiResponse<UpdateSiteResponse> removeSiteResponse =
                api.removeSiteWithHttpInfo(Tester.getAuthorization(), params);
        /*
        //FIXME: Status code should be 404, and not 200.
         */
        assertTrue(removeSiteResponse.getStatusCode() == 200);
    }

}

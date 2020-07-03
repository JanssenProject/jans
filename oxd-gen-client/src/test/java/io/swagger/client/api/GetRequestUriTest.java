package io.swagger.client.api;

import io.swagger.client.model.*;
import org.gluu.oxd.common.Jackson2;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class GetRequestUriTest {

    @Parameters({"redirectUrls", "opHost", "host"})
    @Test
    public void test(String redirectUrls, String opHost, String host) throws Exception {
        DevelopersApi api = Tester.api();

        final RegisterSiteResponse site = RegisterSiteTest.registerSite(api, opHost, redirectUrls);

        //jwks generation
        final GetRpJwksResponse getRpJwksResponse = api.getRpJwks();
        //update site with jwks
        UpdateSiteParams updateSiteParams = new UpdateSiteParams();
        updateSiteParams.setOxdId(site.getOxdId());
        updateSiteParams.setJwks(Jackson2.asJson(getRpJwksResponse));
        updateSiteParams.setRequestObjectSigningAlg("RS256");
        api.updateSite(updateSiteParams, Tester.getAuthorization(site), null);
        //Request uri
        GetRequestObjectUriParams getRequestUriParams = new GetRequestObjectUriParams();
        getRequestUriParams.setOxdId(site.getOxdId());
        getRequestUriParams.setOxdHostUrl(host);
        GetRequestObjectUriResponse getRequestObjectUriResponse = api.getRequestObjectUri(getRequestUriParams, Tester.getAuthorization(site), null);
        assertNotNull(getRequestObjectUriResponse.getRequestUri());


        //Get Request object
        String requestObjectId = getRequestObjectUriResponse.getRequestUri().substring(getRequestObjectUriResponse.getRequestUri().lastIndexOf('/') + 1);
        String requestObject = api.getRequestObject(requestObjectId);
        assertNotNull(requestObject);
    }
}

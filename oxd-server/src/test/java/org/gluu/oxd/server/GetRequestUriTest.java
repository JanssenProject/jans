package org.gluu.oxd.server;

import com.fasterxml.jackson.databind.JsonNode;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.GetAuthorizationUrlParams;
import org.gluu.oxd.common.params.GetRequestObjectUriParams;
import org.gluu.oxd.common.params.UpdateSiteParams;
import org.gluu.oxd.common.response.GetAuthorizationUrlResponse;
import org.gluu.oxd.common.response.GetRequestObjectUriResponse;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.gluu.oxd.server.TestUtils.notEmpty;
import static org.testng.AssertJUnit.assertNotNull;

public class GetRequestUriTest {
    @Parameters({"host", "redirectUrls", "opHost"})
    @Test
    public void test(String host, String redirectUrls, String opHost) {
        ClientInterface client = Tester.newClient(host);
        //client registration
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        //jwks generation
        JsonNode jwks = client.getRpJwks();
        //update jwks in OP
        UpdateSiteParams updateSiteParams = new UpdateSiteParams();
        updateSiteParams.setOxdId(site.getOxdId());
        updateSiteParams.setJwks(jwks.asText());
        updateSiteParams.setRequestObjectSigningAlg("RS256");
        client.updateSite(Tester.getAuthorization(site), null, updateSiteParams);
        //Request uri
        GetRequestObjectUriParams getRequestUriParams = new GetRequestObjectUriParams();
        getRequestUriParams.setOxdId(site.getOxdId());
        getRequestUriParams.setOxdHostUrl("http://localhost" + ":" + SetUpTest.SUPPORT.getLocalPort());
        GetRequestObjectUriResponse getRequestUriResponse = client.getRequestObjectUri(Tester.getAuthorization(site), null, getRequestUriParams);
        assertNotNull(getRequestUriResponse.getRequestUri());
        //Get Request object
        String requestObjectId = getRequestUriResponse.getRequestUri().substring(getRequestUriResponse.getRequestUri().lastIndexOf('/') + 1);
        String requestObject = client.getRequestObject(requestObjectId);
        assertNotNull(requestObject);
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("request", requestObject);

        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setParams(paramsMap);
        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(site), null, commandParams);
        assertNotNull(resp);
        notEmpty(resp.getAuthorizationUrl());
    }
}

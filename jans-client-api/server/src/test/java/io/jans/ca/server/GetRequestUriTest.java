package io.jans.ca.server;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetAuthorizationUrlParams;
import io.jans.ca.common.params.GetRequestObjectUriParams;
import io.jans.ca.common.params.UpdateSiteParams;
import io.jans.ca.common.response.GetAuthorizationUrlResponse;
import io.jans.ca.common.response.GetRequestObjectUriResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static io.jans.ca.server.TestUtils.notEmpty;
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
        updateSiteParams.setRpId(site.getRpId());
        updateSiteParams.setJwks(jwks.asText());
        updateSiteParams.setRequestObjectSigningAlg("RS256");
        client.updateSite(Tester.getAuthorization(site), null, updateSiteParams);
        //Request uri
        GetRequestObjectUriParams getRequestUriParams = new GetRequestObjectUriParams();
        getRequestUriParams.setRpId(site.getRpId());
        getRequestUriParams.setRpHostUrl("http://localhost" + ":" + SetUpTest.SUPPORT.getLocalPort());
        GetRequestObjectUriResponse getRequestUriResponse = client.getRequestObjectUri(Tester.getAuthorization(site), null, getRequestUriParams);
        assertNotNull(getRequestUriResponse.getRequestUri());
        //Get Request object
        String requestObjectId = getRequestUriResponse.getRequestUri().substring(getRequestUriResponse.getRequestUri().lastIndexOf('/') + 1);
        String requestObject = client.getRequestObject(requestObjectId);
        assertNotNull(requestObject);
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("request", requestObject);

        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setRpId(site.getRpId());
        commandParams.setParams(paramsMap);
        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(Tester.getAuthorization(site), null, commandParams);
        assertNotNull(resp);
        TestUtils.notEmpty(resp.getAuthorizationUrl());
    }
}

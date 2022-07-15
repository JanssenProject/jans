package io.jans.ca.server.tests;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetAuthorizationUrlParams;
import io.jans.ca.common.params.GetRequestObjectUriParams;
import io.jans.ca.common.params.UpdateSiteParams;
import io.jans.ca.common.response.GetAuthorizationUrlResponse;
import io.jans.ca.common.response.GetRequestObjectUriResponse;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.server.SetUpTest;
import io.jans.ca.server.TestUtils;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertNotNull;

public class GetRequestUriTest extends BaseTest {
    @ArquillianResource
    URI url;

    @Parameters({"host", "redirectUrls", "opHost"})
    @Test
    public void test(String host, String redirectUrls, String opHost) {
        String hostTargetURL = getApiTagetURL(url);
        ClientInterface client = Tester.newClient(hostTargetURL);
        //client registration
        final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        //jwks generation
        JsonNode jwks = client.getRpJwks();
        //update jwks in OP
        UpdateSiteParams updateSiteParams = new UpdateSiteParams();
        updateSiteParams.setRpId(site.getRpId());
        updateSiteParams.setJwks(jwks.asText());
        updateSiteParams.setRequestObjectSigningAlg("RS256");
        String strAuthorization = Tester.getAuthorization(hostTargetURL, site);
        client.updateSite(strAuthorization, null, updateSiteParams);
        //Request uri
        GetRequestObjectUriParams getRequestUriParams = new GetRequestObjectUriParams();
        getRequestUriParams.setRpId(site.getRpId());
        getRequestUriParams.setRpHostUrl(hostTargetURL);
        GetRequestObjectUriResponse getRequestUriResponse = client.getRequestObjectUri(strAuthorization, getRequestUriParams.getRpId(), getRequestUriParams);
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
        final GetAuthorizationUrlResponse resp = client.getAuthorizationUrl(strAuthorization, commandParams.getRpId(), commandParams);
        assertNotNull(resp);
        TestUtils.notEmpty(resp.getAuthorizationUrl());
    }
}

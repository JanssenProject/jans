package org.xdi.oxd.client.kong;

import com.google.common.base.Strings;
import org.codehaus.jackson.JsonNode;
import org.jboss.resteasy.client.ClientResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;

/**
 * Created by yuriy on 16.10.16.
 */
public class KongTest {

    private static String apiId;

    @Parameters({"kongAdminUrl", "kongApiRequestHost", "kongApiUpstreamUrl"})
    @BeforeClass
    public void setup(String kongAdminUrl, String kongApiRequestHost, String kongApiUpstreamUrl) {
        KongApiService apiService = KongClient.createApiService(kongAdminUrl);
        ClientResponse<JsonNode> addResponse = apiService.addApi(kongApiRequestHost, kongApiRequestHost, null, false, false, kongApiUpstreamUrl);
        System.out.println("POST /apis status: " + addResponse.getStatus() + ", entity: " + addResponse.getEntity());

        apiId = addResponse.getEntity().get("id").asText();
        assertTrue(!Strings.isNullOrEmpty(apiId));
    }

    @Parameters({"kongAdminUrl", "kongApiRequestHost", "kongApiUpstreamUrl"})
    @AfterClass
    public void cleanup(String kongAdminUrl, String kongApiRequestHost, String kongApiUpstreamUrl) {
        KongApiService apiService = KongClient.createApiService(kongAdminUrl);
        ClientResponse<String> deleteResponse = apiService.deleteApi(apiId);
        System.out.println("DELETE /apis status: " + deleteResponse.getStatus() + ", entity: " + deleteResponse.getEntity());

        assertTrue(deleteResponse.getStatus() == Response.Status.NO_CONTENT.getStatusCode());
    }

    @Parameters({"kongAdminUrl", "kongApiRequestHost", "kongProxyUrl", "protectionDocument",
            "oxdHost", "oxdPort", "opHost", "gatScope", "redirectUrl"})
    @Test
    public void test(String kongAdminUrl, String kongApiRequestHost, String kongProxyUrl, String protectionDocument,
                     String oxdHost, int oxdPort, String opHost, String gatScope, String redirectUrl) throws IOException {

        // 1. call without protection
        MockBinService mockBinService = KongClient.createMockBinService(kongProxyUrl);
        ClientResponse<JsonNode> mockResponse = mockBinService.status200Hello(kongApiRequestHost, "");
        System.out.println("GET /status/200 status: " + mockResponse.getStatus() + ", entity: " + mockResponse.getEntity());
        assertTrue(mockResponse.getStatus() == Response.Status.OK.getStatusCode());

        // 2. protect with kong-uma-rs
        KongApiService apiService = KongClient.createApiService(kongAdminUrl);
        ClientResponse<JsonNode> addPluginResponse = apiService.addKongUmaRsPlugin(apiId, "kong-uma-rs", oxdHost, Integer.toString(oxdPort), opHost, protectionDocument);
        System.out.println("POST /{api}/plugins/ status: " + addPluginResponse.getStatus() + ", entity: " + addPluginResponse.getEntity());
        assertTrue(addPluginResponse.getStatus() == Response.Status.CREATED.getStatusCode());

        // 3. call api (must be unauthorized)
        mockResponse = mockBinService.status200Hello(kongApiRequestHost, "");
        System.out.println("GET /status/200 status: " + mockResponse.getStatus() + ", entity: " + mockResponse.getEntity());
        assertTrue(mockResponse.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode());

        // todo - Upgrade kong. Kong supports UMA 1.0.1 (it does not support UMA 2).
        // 4. obtain GAT with correct scope (gatScope) -
        String gat = "";//getGat(oxdHost, oxdPort, opHost, redirectUrl, gatScope);
//        System.out.println("GAT: " + gat);

        // 5. call api (must be unauthorized)
        mockResponse = mockBinService.status200Hello(kongApiRequestHost, "Bearer " + gat);
        System.out.println("GET /status/200 status: " + mockResponse.getStatus() + ", entity: " + mockResponse.getEntity());
        assertTrue(mockResponse.getStatus() == Response.Status.OK.getStatusCode());
    }
}

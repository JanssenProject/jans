package org.xdi.oxd.client.kong;

import junit.framework.Assert;
import org.codehaus.jackson.JsonNode;
import org.jboss.resteasy.client.ClientResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.client.RegisterSiteTest;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;
import org.xdi.oxd.common.response.GetAuthorizationUrlResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * Created by yuriy on 16.10.16.
 */
public class KongTest {

    @Parameters({"kongAdminUrl"})
    @BeforeClass
    public void setup(String kongAdminUrl) {
        KongApiService kong = KongClient.createApiService(kongAdminUrl);
        //kong.addApi()
    }

    @Parameters({"kongAdminUrl"})
    @Test
    public void test(String kongAdminUrl) throws IOException {
        KongApiService apiService = KongClient.createApiService(kongAdminUrl);

        ClientResponse<JsonNode> getResponse = apiService.getApis();
        System.out.println("GET /apis status: " + getResponse.getStatus() + ", entity: " + getResponse.getEntity());

        ClientResponse<String> deleteResponse = apiService.deleteApi("b1fdd250-6152-4f7d-880e-7a09255e9b7b");
        System.out.println("DELETE /apis status: " + deleteResponse.getStatus() + ", entity: " + deleteResponse.getEntity());

    }


    @Parameters({"host", "port", "redirectUrl", "opHost"})
    @Test
    public void kongUmaRs(String host, int port, String redirectUrl, String opHost) throws IOException {

        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
            commandParams.setOxdId(site.getOxdId());

            final Command command = new Command(CommandType.GET_AUTHORIZATION_URL);
            command.setParamsObject(commandParams);

            final GetAuthorizationUrlResponse resp = client.send(command).dataAsResponse(GetAuthorizationUrlResponse.class);
            assertNotNull(resp);
            notEmpty(resp.getAuthorizationUrl());
            Assert.assertTrue(resp.getAuthorizationUrl().contains("acr_values"));
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

}

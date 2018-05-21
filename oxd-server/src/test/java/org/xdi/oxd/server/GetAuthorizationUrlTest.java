package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.client.OxdClient;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class GetAuthorizationUrlTest {

    public static void main(String[] args) {
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId("siteIdHere");

        ClientInterface clientInterface = OxdClient.newClient("http://localhost:8084");
        CommandResponse response = clientInterface.getAuthorizationUrl("", commandParams);
        System.out.println(response);
    }

    @Parameters({"host", "port", "redirectUrl", "opHost"})
    @Test
    public void test(String host, int port, String redirectUrl, String opHost) throws IOException {
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId("siteIdHere");

        ClientInterface clientInterface = OxdClient.newClient("http://localhost:8084");
        CommandResponse response = clientInterface.getAuthorizationUrl("", commandParams);

        try {

//            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            //final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
            //commandParams.setOxdId(site.getOxdId());

            //final Command command = new Command(CommandType.GET_AUTHORIZATION_URL);
            //command.setParamsObject(commandParams);

            //final GetAuthorizationUrlResponse resp = client.send(command).dataAsResponse(GetAuthorizationUrlResponse.class);
            //assertNotNull(resp);
            //notEmpty(resp.getAuthorizationUrl());
        } finally {
            //CommandClient.closeQuietly(client);
        }
    }
}

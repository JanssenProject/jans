package org.xdi.oxd.client;

import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;
import org.xdi.oxd.common.response.GetAuthorizationUrlResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class GetAuthorizationUrlTest {

    @Parameters({"host", "port", "redirectUrl"})
    @Test
    public void test(String host, int port, String redirectUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, redirectUrl);

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

package org.xdi.oxd.client;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.LogoutParams;
import org.xdi.oxd.common.response.LogoutResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/11/2015
 */

public class LogoutTest {

    @Parameters({"host", "port", "redirectUrl", "userId", "userSecret"})
    @Test
    public void test(String host, int port, String redirectUrl, String userId, String userSecret) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, redirectUrl);

            GetTokensByCodeTest.tokenByCode(client, site, redirectUrl, userId, userSecret);

            final LogoutParams commandParams = new LogoutParams();
            commandParams.setOxdId(site.getSiteId());

            final Command command = new Command(CommandType.LOGOUT).setParamsObject(commandParams);

            final LogoutResponse resp = client.send(command).dataAsResponse(LogoutResponse.class);
            assertNotNull(resp);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}

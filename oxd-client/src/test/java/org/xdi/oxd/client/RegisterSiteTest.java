package org.xdi.oxd.client;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.client.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2015
 */

public class RegisterSiteTest {

    @Parameters({"host", "port", "redirectUrl"})
    @Test
    public void test(String host, int port, String redirectUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse resp = registerSite(client, redirectUrl);
            assertNotNull(resp);

            notEmpty(resp.getSiteId());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RegisterSiteResponse registerSite(CommandClient client, String redirectUrl) {
        return registerSite(client, redirectUrl, redirectUrl);
    }

    public static RegisterSiteResponse registerSite(CommandClient client, String redirectUrl, String logoutRedirectUrl) {
        final RegisterSiteParams commandParams = new RegisterSiteParams();
        commandParams.setAuthorizationRedirectUri(redirectUrl);
        commandParams.setLogoutRedirectUri(logoutRedirectUrl);

        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(commandParams);

        final RegisterSiteResponse resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
        assertNotNull(resp);
        return resp;
    }

}

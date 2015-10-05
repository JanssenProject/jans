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

    @Parameters({"host", "port"})
    @Test
    public void test(String host, int port) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteParams commandParams = new RegisterSiteParams();
            commandParams.setAuthorizationRedirectUri("http://site.example.com");

            final Command command = new Command(CommandType.REGISTER_SITE);
            command.setParamsObject(commandParams);

            final RegisterSiteResponse resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
            assertNotNull(resp);

            notEmpty(resp.getSiteId());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }


}

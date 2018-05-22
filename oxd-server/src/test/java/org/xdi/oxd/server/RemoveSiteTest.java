package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RemoveSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RemoveSiteResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author yuriyz
 */
public class RemoveSiteTest {

    @Parameters({"host", "port", "opHost", "redirectUrl"})
    @Test
    public void removeSiteTest(String host, int port, String opHost, String redirectUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse resp = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
            assertNotNull(resp);

            notEmpty(resp.getOxdId());

            final Command command = new Command(CommandType.REMOVE_SITE).setParamsObject(new RemoveSiteParams(resp.getOxdId()));

            RemoveSiteResponse removeResponse = client.send(command).dataAsResponse(RemoveSiteResponse.class);
            assertNotNull(removeResponse);
            assertNotNull(removeResponse.getOxdId());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}

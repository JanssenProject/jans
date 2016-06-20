package org.xdi.oxd.client;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsProtectResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 10/06/2016
 */

public class RsProtectTest {

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void protect(String host, int port, String redirectUrl, String opHost, String rsProtect) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            final RsProtectParams commandParams = new RsProtectParams();
            commandParams.setOxdId(site.getOxdId());
            commandParams.setResources(UmaFullTest.resourceList(rsProtect).getResources());

            final Command command = new Command(CommandType.RS_PROTECT)
                    .setParamsObject(commandParams);

            final RsProtectResponse resp = client.send(command).dataAsResponse(RsProtectResponse.class);
            assertNotNull(resp);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}

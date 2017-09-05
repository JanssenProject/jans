package org.xdi.oxd.client;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsProtectResponse;
import org.xdi.oxd.rs.protect.RsResource;

import java.io.IOException;
import java.util.List;

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

            protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RsProtectResponse protectResources(CommandClient client, RegisterSiteResponse site, List<RsResource> resources) {
        final RsProtectParams commandParams = new RsProtectParams();
        commandParams.setOxdId(site.getOxdId());
        commandParams.setResources(resources);

        final RsProtectResponse resp = client
                .send(new Command(CommandType.RS_PROTECT).setParamsObject(commandParams))
                .dataAsResponse(RsProtectResponse.class);
        assertNotNull(resp);
        return resp;
    }
}

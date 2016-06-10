package org.xdi.oxd.client;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.oxd.common.response.RsProtectResponse;
import org.xdi.oxd.rs.protect.Jackson;
import org.xdi.oxd.rs.protect.RsResourceList;

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
            commandParams.setResources(resourceList(rsProtect));

            final Command command = new Command(CommandType.RS_PROTECT)
                    .setParamsObject(commandParams);

            final RsProtectResponse resp = client.send(command).dataAsResponse(RsProtectResponse.class);
            assertNotNull(resp);

            //checkAccessPositive(client);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private void checkAccessPositive(CommandClient client) {
        RsCheckAccessParams params = new RsCheckAccessParams();
        final Command command = new Command(CommandType.RS_CHECK_ACCESS)
                .setParamsObject(params);

        final RsCheckAccessResponse resp = client.send(command).dataAsResponse(RsCheckAccessResponse.class);
        assertNotNull(resp);
    }

    private RsResourceList resourceList(String rsProtect) throws IOException {
        rsProtect = StringUtils.replace(rsProtect, "'", "\"");
        return Jackson.createJsonMapper().readValue(rsProtect, RsResourceList.class);
    }
}

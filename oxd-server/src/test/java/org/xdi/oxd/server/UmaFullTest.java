package org.xdi.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RpGetRptParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RpGetRptResponse;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.oxd.rs.protect.Jackson;
import org.xdi.oxd.rs.protect.RsResourceList;

import java.io.IOException;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/06/2016
 */

public class UmaFullTest {

    private RegisterSiteResponse site;
    private CommandClient client;

    @Parameters({"host", "port", "redirectUrl", "opHost", "rsProtect"})
    @Test
    public void test(String host, int port, String redirectUrl, String opHost, String rsProtect) throws Exception {
        this.client = null;
        try {
            this.client = new CommandClient(host, port);

            site = RegisterSiteTest.registerSite(this.client, opHost, redirectUrl);

            RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

            final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

            final RpGetRptParams params = new RpGetRptParams();
            params.setOxdId(site.getOxdId());
            params.setTicket(checkAccess.getTicket());

            final RpGetRptResponse response = client
                    .send(new Command(CommandType.RP_GET_RPT).setParamsObject(params))
                    .dataAsResponse(RpGetRptResponse.class);

            assertNotNull(response);
            assertTrue(StringUtils.isNotBlank(response.getRpt()));
            assertTrue(StringUtils.isNotBlank(response.getPct()));


        } finally {
            CommandClient.closeQuietly(this.client);
        }
    }

    public static RsResourceList resourceList(String rsProtect) throws IOException {
        rsProtect = StringUtils.replace(rsProtect, "'", "\"");
        return Jackson.createJsonMapper().readValue(rsProtect, RsResourceList.class);
    }
}

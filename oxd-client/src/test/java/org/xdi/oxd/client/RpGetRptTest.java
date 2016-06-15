package org.xdi.oxd.client;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RpGetRptParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RpGetRptResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class RpGetRptTest {

    @Parameters({"host", "port", "opHost", "redirectUrl"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            final RpGetRptParams params = new RpGetRptParams();
            params.setOxdId(site.getOxdId());

            final Command command = new Command(CommandType.RP_GET_RPT);
            command.setParamsObject(params);
            final CommandResponse response = client.send(command);
            Assert.assertNotNull(response);
            System.out.println(response);

            final RpGetRptResponse rptResponse = response.dataAsResponse(RpGetRptResponse.class);
            Assert.assertNotNull(rptResponse);
            Assert.assertTrue(StringUtils.isNotBlank(rptResponse.getRpt()));
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}

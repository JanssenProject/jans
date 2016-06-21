package org.xdi.oxd.client;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RpGetGatParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RpGetRptResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/06/2016
 */

public class RpGetGatTest {

    @Parameters({"host", "port", "opHost", "redirectUrl"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            final RpGetGatParams params = new RpGetGatParams();
            params.setOxdId(site.getOxdId());
            params.setScopes(Lists.newArrayList("http://photoz.example.com/dev/actions/all"));

            final Command command = new Command(CommandType.RP_GET_GAT);
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

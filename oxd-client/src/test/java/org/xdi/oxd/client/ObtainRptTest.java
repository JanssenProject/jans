package org.xdi.oxd.client;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.ObtainRptParams;
import org.xdi.oxd.common.response.ObtainAatOpResponse;
import org.xdi.oxd.common.response.ObtainRptOpResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class ObtainRptTest {

    @Parameters({"host", "port", "discoveryUrl", "umaDiscoveryUrl", "redirectUrl",
            "clientId", "clientSecret", "userId", "userSecret", "amHost"})
    @Test
    public void test(String host, int port, String discoveryUrl, String umaDiscoveryUrl, String redirectUrl,
                     String clientId, String clientSecret, String userId, String userSecret, String amHost) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            final ObtainAatOpResponse r = TestUtils.obtainAat(client, discoveryUrl, umaDiscoveryUrl, redirectUrl,
                    clientId, clientSecret, userId, userSecret);
            Assert.assertNotNull(r);
            final String aatToken = r.getAatToken();

            final ObtainRptParams params = new ObtainRptParams();
            params.setAat(aatToken);
            params.setAmHost(amHost);

            final Command command = new Command(CommandType.OBTAIN_RPT);
            command.setParamsObject(params);
            final CommandResponse response = client.send(command);
            Assert.assertNotNull(response);
            System.out.println(response);

            final ObtainRptOpResponse rptResponse = response.dataAsResponse(ObtainRptOpResponse.class);
            Assert.assertNotNull(rptResponse);
            Assert.assertTrue(StringUtils.isNotBlank(rptResponse.getRptToken()));
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}

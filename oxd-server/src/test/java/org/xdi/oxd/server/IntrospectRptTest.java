package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.xdi.oxd.common.params.IntrospectRptParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RpGetRptResponse;

import java.io.IOException;

import static junit.framework.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

/**
 * @author yuriyz
 */
public class IntrospectRptTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "rsProtect"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl, String rsProtect) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
            final RpGetRptResponse rptResponse = RpGetRptTest.requestRpt(client, site, rsProtect);

            IntrospectRptParams params = new IntrospectRptParams();
            params.setOxdId(site.getOxdId());
            params.setRpt(rptResponse.getRpt());

            final CorrectRptIntrospectionResponse response = client
                    .send(new Command(CommandType.INTROSPECT_RPT).setParamsObject(params))
                    .dataAsResponse(CorrectRptIntrospectionResponse.class);

            assertNotNull(response);
            assertTrue(response.getActive());
            assertTrue(response.getExpiresAt() != null);
            assertTrue(response.getIssuedAt() != null);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}

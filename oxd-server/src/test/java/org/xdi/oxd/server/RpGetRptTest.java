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

import java.io.IOException;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class RpGetRptTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "rsProtect"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl, String rsProtect) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
            final RpGetRptResponse response = requestRpt(client, site, rsProtect);

            assertNotNull(response);

//            ErrorResponse errorResponse = commandResponse.dataAsResponse(ErrorResponse.class);
//            assertNotNull(errorResponse);
//
//            // expecting need_info error
//            UmaNeedInfoResponse needInfo = errorResponse.detailsAs(UmaNeedInfoResponse.class);
//            assertNotNull(needInfo);
//            assertTrue(StringUtils.isNotBlank(needInfo.getTicket()));
//            assertTrue(StringUtils.isNotBlank(needInfo.getRedirectUser()));
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RpGetRptResponse requestRpt(CommandClient client, RegisterSiteResponse site, String rsProtect) throws IOException {
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
        return response;
    }
}

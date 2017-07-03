package org.xdi.oxd.client;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.model.uma.UmaNeedInfoResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.ErrorResponse;
import org.xdi.oxd.common.params.RpGetRptParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsCheckAccessResponse;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

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

            RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

            final RsCheckAccessResponse checkAccess = RsCheckAccessTest.checkAccess(client, site);

            final RpGetRptParams params = new RpGetRptParams();
            params.setOxdId(site.getOxdId());
            params.setTicket(checkAccess.getTicket());

            final CommandResponse commandResponse = client.send(new Command(CommandType.RP_GET_RPT).setParamsObject(params));

            ErrorResponse errorResponse = commandResponse.dataAsResponse(ErrorResponse.class);
            assertNotNull(errorResponse);

            // expecting need_info error
            UmaNeedInfoResponse needInfo = errorResponse.detailsAs(UmaNeedInfoResponse.class);
            assertNotNull(needInfo);
            assertTrue(StringUtils.isNotBlank(needInfo.getTicket()));
            assertTrue(StringUtils.isNotBlank(needInfo.getRedirectUser()));
        } finally {
            CommandClient.closeQuietly(client);
        }
    }
}

package org.xdi.oxd.server;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsCheckAccessResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/07/2017
 */

public class RsCheckAccessTest {

    @Parameters({"host", "port", "opHost", "redirectUrl", "rsProtect"})
    @Test
    public void test(String host, int port, String opHost, String redirectUrl, String rsProtect) throws IOException {
        CommandClient client = null;
        try {
            client = new CommandClient(host, port);

            RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);

            RsProtectTest.protectResources(client, site, UmaFullTest.resourceList(rsProtect).getResources());

            checkAccess(client, site);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RsCheckAccessResponse checkAccess(CommandClient client, RegisterSiteResponse site) {
        final RsCheckAccessParams params = new RsCheckAccessParams();
        params.setOxdId(site.getOxdId());
        params.setHttpMethod("GET");
        params.setPath("/ws/phone");
        params.setRpt("dummy");

        final RsCheckAccessResponse response = client
                .send(new Command(CommandType.RS_CHECK_ACCESS).setParamsObject(params))
                .dataAsResponse(RsCheckAccessResponse.class);

        Assert.assertNotNull(response);
        Assert.assertTrue(StringUtils.isNotBlank(response.getAccess()));
        return response;
    }
}

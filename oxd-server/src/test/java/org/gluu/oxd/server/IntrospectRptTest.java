package org.gluu.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.gluu.oxd.common.params.IntrospectRptParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RpGetRptResponse;

import java.io.IOException;

import static junit.framework.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

/**
 * @author yuriyz
 */
public class IntrospectRptTest {

    @Parameters({"host", "opHost", "redirectUrl", "rsProtect"})
    @Test
    public void test(String host, String opHost, String redirectUrl, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        final RpGetRptResponse rptResponse = RpGetRptTest.requestRpt(client, site, rsProtect);

        IntrospectRptParams params = new IntrospectRptParams();
        params.setOxdId(site.getOxdId());
        params.setRpt(rptResponse.getRpt());

        final CorrectRptIntrospectionResponse response = client.introspectRpt(Tester.getAuthorization(), params);

        assertNotNull(response);
        assertTrue(response.getActive());
        assertTrue(response.getExpiresAt() != null);
        assertTrue(response.getIssuedAt() != null);
    }
}

package io.jans.ca.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.IntrospectRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;

import java.io.IOException;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.Assert.assertNotNull;

/**
 * @author yuriyz
 */
public class IntrospectRptTest {

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void test(String host, String opHost, String redirectUrls, String rsProtect) throws IOException {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final RpGetRptResponse rptResponse = RpGetRptTest.requestRpt(client, site, rsProtect);

        IntrospectRptParams params = new IntrospectRptParams();
        params.setOxdId(site.getOxdId());
        params.setRpt(rptResponse.getRpt());

        final CorrectRptIntrospectionResponse response = client.introspectRpt(Tester.getAuthorization(site), null, params);

        assertNotNull(response);
        assertTrue(response.getActive());
        assertTrue(response.getExpiresAt() != null);
        assertTrue(response.getIssuedAt() != null);
    }
}

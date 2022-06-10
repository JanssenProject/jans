package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.IntrospectRptParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RpGetRptResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;

import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author yuriyz
 */
public class IntrospectRptTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "redirectUrls", "rsProtect"})
    @Test
    public void test(String host, String opHost, String redirectUrls, String rsProtect) throws IOException {
        ClientInterface client = getClientInterface(url);

        RegisterSiteResponse site = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        final RpGetRptResponse rptResponse = RpGetRptTest.requestRpt(client, site, rsProtect);

        IntrospectRptParams params = new IntrospectRptParams();
        params.setRpId(site.getRpId());
        params.setRpt(rptResponse.getRpt());

        final CorrectRptIntrospectionResponse response = client.introspectRpt(Tester.getAuthorization(client.getApitargetURL(), site), null, params);

        assertNotNull(response);
        assertTrue(response.getActive());
        assertTrue(response.getExpiresAt() != null);
        assertTrue(response.getIssuedAt() != null);
    }
}

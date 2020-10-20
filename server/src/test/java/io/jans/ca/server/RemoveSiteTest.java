package io.jans.ca.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.RemoveSiteParams;
import io.jans.ca.common.response.RegisterSiteResponse;
import io.jans.ca.common.response.RemoveSiteResponse;

import static org.testng.AssertJUnit.assertNotNull;
import static io.jans.ca.server.TestUtils.notEmpty;

/**
 * @author yuriyz
 */
public class RemoveSiteTest {

    @Parameters({"host", "opHost", "redirectUrls"})
    @Test
    public void removeSiteTest(String host, String opHost, String redirectUrls) {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse resp = RegisterSiteTest.registerSite(client, opHost, redirectUrls);
        assertNotNull(resp);

        notEmpty(resp.getOxdId());

        RemoveSiteResponse removeResponse = client.removeSite(Tester.getAuthorization(resp), null, new RemoveSiteParams(resp.getOxdId()));
        assertNotNull(removeResponse);
        assertNotNull(removeResponse.getOxdId());
    }
}

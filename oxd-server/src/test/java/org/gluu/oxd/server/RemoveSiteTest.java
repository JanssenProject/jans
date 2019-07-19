package org.gluu.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.RemoveSiteParams;
import org.gluu.oxd.common.response.RegisterSiteResponse;
import org.gluu.oxd.common.response.RemoveSiteResponse;

import static junit.framework.Assert.assertNotNull;
import static org.gluu.oxd.server.TestUtils.notEmpty;

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

        RemoveSiteResponse removeResponse = client.removeSite(Tester.getAuthorization(), new RemoveSiteParams(resp.getOxdId()));
        assertNotNull(removeResponse);
        assertNotNull(removeResponse.getOxdId());
    }
}

package org.xdi.oxd.server;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.params.RemoveSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RemoveSiteResponse;

import static junit.framework.Assert.assertNotNull;
import static org.xdi.oxd.server.TestUtils.notEmpty;

/**
 * @author yuriyz
 */
public class RemoveSiteTest {

    @Parameters({"host", "opHost", "redirectUrl"})
    @Test
    public void removeSiteTest(String host, String opHost, String redirectUrl) {
        ClientInterface client = Tester.newClient(host);

        RegisterSiteResponse resp = RegisterSiteTest.registerSite(client, opHost, redirectUrl);
        assertNotNull(resp);

        notEmpty(resp.getOxdId());

        RemoveSiteResponse removeResponse = client.removeSite(Tester.getAuthorization(), new RemoveSiteParams(resp.getOxdId()));
        assertNotNull(removeResponse);
        assertNotNull(removeResponse.getOxdId());
    }
}

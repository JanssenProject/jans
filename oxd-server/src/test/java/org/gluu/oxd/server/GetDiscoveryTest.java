package org.gluu.oxd.server;

import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.GetDiscoveryParams;
import org.gluu.oxd.common.response.GetDiscoveryResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class GetDiscoveryTest {
    @Parameters({"host", "opHost", "opDiscoveryPath"})
    @Test
    public void test(String host, String opHost, String opDiscoveryPath) {
        ClientInterface client = Tester.newClient(host);

        final GetDiscoveryParams commandParams = new GetDiscoveryParams();
        commandParams.setOpHost(opHost);
        commandParams.setOpDiscoveryPath(opDiscoveryPath);

        final GetDiscoveryResponse resp = client.getDiscovery(commandParams);
        assertNotNull(resp);
        assertNotNull(resp.getIssuer());
    }
}

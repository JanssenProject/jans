package org.gluu.oxd.server;

import org.gluu.oxd.client.ClientInterface;
import org.gluu.oxd.common.params.GetOpDiscoveryConfigParams;
import org.gluu.oxd.common.response.GetOpDiscoveryConfigResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

public class GetOpDiscoveryConfigTest {
    @Parameters({"host", "opHost", "opDiscoveryPath"})
    @Test
    public void test(String host, String opHost, String opDiscoveryPath) {
        ClientInterface client = Tester.newClient(host);

        final GetOpDiscoveryConfigParams commandParams = new GetOpDiscoveryConfigParams();
        commandParams.setOpHost(opHost);
        commandParams.setOpDiscoveryPath(opDiscoveryPath);

        final GetOpDiscoveryConfigResponse resp = client.getOpDiscoveryConfig(commandParams);
        assertNotNull(resp);
        assertNotNull(resp.getIssuer());
    }
}

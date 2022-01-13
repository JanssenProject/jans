package io.jans.ca.server;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetDiscoveryParams;
import io.jans.ca.common.response.GetDiscoveryResponse;
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

    @Parameters({"host", "opConfigurationEndpoint"})
    @Test
    public void test_withOpConfigurationEndpoint(String host, String opConfigurationEndpoint) {
        ClientInterface client = Tester.newClient(host);

        final GetDiscoveryParams commandParams = new GetDiscoveryParams();
        commandParams.setOpConfigurationEndpoint(opConfigurationEndpoint);

        final GetDiscoveryResponse resp = client.getDiscovery(commandParams);
        assertNotNull(resp);
        assertNotNull(resp.getIssuer());
    }
}

package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetDiscoveryParams;
import io.jans.ca.common.response.GetDiscoveryResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.AssertJUnit.assertNotNull;

public class GetDiscoveryTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"host", "opHost", "opDiscoveryPath"})
    @Test
    public void test(String host, String opHost, String opDiscoveryPath) {
        ClientInterface client = getClientInterface(url);

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
        ClientInterface client = getClientInterface(url);

        final GetDiscoveryParams commandParams = new GetDiscoveryParams();
        commandParams.setOpConfigurationEndpoint(opConfigurationEndpoint);

        final GetDiscoveryResponse resp = client.getDiscovery(commandParams);
        assertNotNull(resp);
        assertNotNull(resp.getIssuer());
    }
}

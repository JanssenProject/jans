package io.swagger.client.api;

import io.swagger.client.model.GetDiscoveryParams;
import io.swagger.client.model.GetDiscoveryResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertNotNull;

public class GetDiscoveryTest {
    @Parameters({"opHost", "opDiscoveryPath"})
    @Test
    public void test(String opHost, String opDiscoveryPath) throws Exception {
        DevelopersApi api = Tester.api();

        final GetDiscoveryParams commandParams = new GetDiscoveryParams();
        commandParams.setOpHost(opHost);
        commandParams.setOpDiscoveryPath(opDiscoveryPath);

        final GetDiscoveryResponse resp = api.getDiscovery(commandParams);
        assertNotNull(resp);
        assertNotNull(resp.getIssuer());
    }
}

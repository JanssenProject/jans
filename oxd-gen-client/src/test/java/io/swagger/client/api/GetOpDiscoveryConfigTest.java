package io.swagger.client.api;

import io.swagger.client.model.GetOpDiscoveryConfigParams;
import io.swagger.client.model.GetOpDiscoveryConfigResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertNotNull;

public class GetOpDiscoveryConfigTest {
    @Parameters({"opHost", "opDiscoveryPath"})
    @Test
    public void test(String opHost, String opDiscoveryPath) throws Exception {
            DevelopersApi api = Tester.api();

            final GetOpDiscoveryConfigParams commandParams = new GetOpDiscoveryConfigParams();
            commandParams.setOpHost(opHost);
            commandParams.setOpDiscoveryPath(opDiscoveryPath);

            final GetOpDiscoveryConfigResponse resp = api.getOpDiscoveryConfig(commandParams);
            assertNotNull(resp);
            assertNotNull(resp.getIssuer());
    }
}

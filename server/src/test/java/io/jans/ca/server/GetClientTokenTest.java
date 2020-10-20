package io.jans.ca.server;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import io.jans.ca.common.params.GetClientTokenParams;
import io.jans.ca.common.response.GetClientTokenResponse;

import static org.testng.AssertJUnit.assertNotNull;
import static io.jans.ca.server.TestUtils.notEmpty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/03/2017
 */

public class GetClientTokenTest {

    @Parameters({"host", "opHost"})
    @Test
    public void getClientToken(String host, String opHost) {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(opHost);
        params.setScope(Lists.newArrayList("openid"));
        params.setClientId(Tester.getSetupClient().getClientId());
        params.setClientSecret(Tester.getSetupClient().getClientSecret());

        GetClientTokenResponse resp = Tester.newClient(host).getClientToken(params);

        assertNotNull(resp);
        notEmpty(resp.getAccessToken());

    }

    @Parameters({"host", "opConfigurationEndpoint"})
    @Test
    public void getClientToken_withOpConfigurationEndpoint(String host, String opConfigurationEndpoint) {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpConfigurationEndpoint(opConfigurationEndpoint);
        params.setScope(Lists.newArrayList("openid"));
        params.setClientId(Tester.getSetupClient().getClientId());
        params.setClientSecret(Tester.getSetupClient().getClientSecret());

        GetClientTokenResponse resp = Tester.newClient(host).getClientToken(params);

        assertNotNull(resp);
        notEmpty(resp.getAccessToken());

    }
}

package org.gluu.oxd.server;

import com.google.common.collect.Lists;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.gluu.oxd.common.params.GetClientTokenParams;
import org.gluu.oxd.common.response.GetClientTokenResponse;

import static junit.framework.Assert.assertNotNull;
import static org.gluu.oxd.server.TestUtils.notEmpty;

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
}

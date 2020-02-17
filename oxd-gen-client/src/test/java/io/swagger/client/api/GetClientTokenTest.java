package io.swagger.client.api;

import com.google.common.collect.Lists;
import io.swagger.client.ApiException;
import io.swagger.client.model.GetClientTokenParams;
import io.swagger.client.model.GetClientTokenResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;



/**
 * @author Yuriy Zabrovarnyy
 * @author Shoeb
 * @version 9/17/2018
 */

public class GetClientTokenTest {

    @Parameters({ "opHost"})
    @Test
    public void getClientToken(String opHost) throws ApiException {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpHost(opHost);
        params.setScope(Lists.newArrayList("openid","oxd"));
        params.setClientId(Tester.getSetupData().getClientId());
        params.setClientSecret(Tester.getSetupData().getClientSecret());

        GetClientTokenResponse resp = Tester.api().getClientToken(params);
        assertNotNull(resp);
        Tester.notEmpty(resp.getAccessToken());

    }

    @Parameters({ "opConfigurationEndpoint"})
    @Test
    public void getClientToken_withOpConfigurationEndpoint(String opConfigurationEndpoint) throws ApiException {
        final GetClientTokenParams params = new GetClientTokenParams();
        params.setOpConfigurationEndpoint(opConfigurationEndpoint);
        params.setScope(Lists.newArrayList("openid","oxd"));
        params.setClientId(Tester.getSetupData().getClientId());
        params.setClientSecret(Tester.getSetupData().getClientSecret());

        GetClientTokenResponse resp = Tester.api().getClientToken(params);
        assertNotNull(resp);
        Tester.notEmpty(resp.getAccessToken());

    }
}
